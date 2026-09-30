package com.example

import com.example.ui.components.CustomerReportOptions
import com.example.ui.components.ReportCustomField
import com.example.ui.components.ReportPdfLayout
import com.example.util.GridAlign
import com.example.util.GridColumnKey
import com.example.util.QuotationGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ruled (GST / e-way bill style) quotation is a grid whose columns come from the sender's
 * toggles. Switch four of them off and the widths, the cell edges and the position of every total
 * have to move together — which is the one thing in a hand-drawn PDF that fails silently, because a
 * column half a point wide still "works", it just prints nothing anybody can read.
 *
 * The drawing itself cannot be checked here: `android.graphics.pdf.PdfDocument` needs the real
 * Android runtime and refuses to open a document under Robolectric, so the geometry was pulled out
 * into [QuotationGrid] where it is plain numbers. What follows checks those numbers. The visual
 * result still has to be looked at on a phone.
 */
class RuledFormPdfTest {

    /** The page the form is drawn on: A4 at 595pt wide, with a 24pt margin each side. */
    private val left = 24f
    private val right = 571f

    private fun grid(options: CustomerReportOptions) = QuotationGrid.forOptions(options, left, right)

    /** Every configuration the toggles can produce, so no test has to guess which one is awkward. */
    private fun allConfigurations(): List<Pair<String, CustomerReportOptions>> = buildList {
        val flags = listOf(true, false)
        for (brand in flags) for (salesman in flags) for (status in flags) for (gst in flags) {
            add(
                "brand=$brand salesman=$salesman status=$status gst=$gst" to CustomerReportOptions(
                    layout = ReportPdfLayout.RULED_FORM,
                    showBrand = brand,
                    showSalesman = salesman,
                    showStatus = status,
                    showGstColumn = gst
                )
            )
        }
    }

    // ── Edges ────────────────────────────────────────────────────────────────

    @Test
    fun `grid always spans exactly the page margins`() {
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            assertEquals("left edge drifted for $name", left, g.edges.first(), 0.01f)
            assertEquals("right edge drifted for $name", right, g.edges.last(), 0.01f)
        }
    }

    @Test
    fun `edges never run backwards or repeat`() {
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            for (i in 1 until g.edges.size) {
                assertTrue(
                    "edge $i is not past edge ${i - 1} for $name (${g.edges[i - 1]} -> ${g.edges[i]})",
                    g.edges[i] > g.edges[i - 1]
                )
            }
        }
    }

    @Test
    fun `there is one more edge than there are columns`() {
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            assertEquals("edge count wrong for $name", g.columns.size + 1, g.edges.size)
        }
    }

    /**
     * Twelve columns is the widest the grid gets. The narrowest of them still has to hold a two digit
     * serial number at 6.1pt, which needs roughly 8pt of room after the padding.
     */
    @Test
    fun `every cell has usable room even in the widest grid`() {
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            g.columns.indices.forEach { index ->
                assertTrue(
                    "column ${g.columns[index].key} has only ${g.cellWidth(index)}pt of room for $name",
                    g.cellWidth(index) >= 8f
                )
            }
        }
    }

    @Test
    fun `column widths follow their weights`() {
        val g = grid(CustomerReportOptions(showGstColumn = true))
        val weightSum = g.columns.fold(0f) { acc, c -> acc + c.weight }
        val pointsPerWeight = (right - left) / weightSum
        g.columns.forEachIndexed { index, column ->
            val drawn = g.edges[index + 1] - g.edges[index]
            assertEquals(
                "${column.key} is not proportional to its weight",
                column.weight * pointsPerWeight,
                drawn,
                0.05f
            )
        }
    }

    // ── Which columns appear ─────────────────────────────────────────────────

    @Test
    fun `the eight columns a quotation cannot do without are always present`() {
        val alwaysThere = listOf(
            GridColumnKey.SR,
            GridColumnKey.ORDER_NO,
            GridColumnKey.ITEM,
            GridColumnKey.PCS,
            GridColumnKey.RATE,
            GridColumnKey.TAXABLE,
            GridColumnKey.PACK,
            GridColumnKey.DATE
        )
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            alwaysThere.forEach { key ->
                assertTrue("$key went missing for $name", g.indexOf(key) >= 0)
            }
        }
    }

    @Test
    fun `optional columns appear only when switched on`() {
        val optional = mapOf(
            GridColumnKey.BRAND to { o: CustomerReportOptions -> o.showBrand },
            GridColumnKey.SALESMAN to { o: CustomerReportOptions -> o.showSalesman },
            GridColumnKey.STATUS to { o: CustomerReportOptions -> o.showStatus },
            GridColumnKey.GST to { o: CustomerReportOptions -> o.showGstColumn }
        )
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            optional.forEach { (key, isOn) ->
                assertEquals("$key does not match its switch for $name", isOn(options), g.indexOf(key) >= 0)
            }
        }
    }

    /** A switched-off column reports -1, which is what tells the total row to skip that figure. */
    @Test
    fun `a switched off column reports no index rather than zero`() {
        val g = grid(CustomerReportOptions(showGstColumn = false, showStatus = false))
        assertEquals(-1, g.indexOf(GridColumnKey.GST))
        assertEquals(-1, g.indexOf(GridColumnKey.STATUS))
        assertTrue(g.indexOf(GridColumnKey.SR) == 0)
    }

    /**
     * The total row prints its figures by column index, so those indices have to keep pointing at the
     * heading they belong under when earlier columns are dropped.
     */
    @Test
    fun `total figures stay under their own headings when columns are dropped`() {
        allConfigurations().forEach { (name, options) ->
            val g = grid(options)
            listOf(GridColumnKey.PCS, GridColumnKey.TAXABLE, GridColumnKey.GST, GridColumnKey.PACK)
                .forEach { key ->
                    val index = g.indexOf(key)
                    if (index >= 0) {
                        assertEquals("total for $key lands on the wrong column for $name", key, g.columns[index].key)
                    }
                }
        }
    }

    @Test
    fun `dropping the brand column really narrows the grid`() {
        val withBrand = grid(CustomerReportOptions(showBrand = true))
        val without = grid(CustomerReportOptions(showBrand = false))
        assertEquals(withBrand.columns.size - 1, without.columns.size)
        // The same page width shared between fewer columns means each survivor gets more room
        val itemWith = withBrand.cellWidth(withBrand.indexOf(GridColumnKey.ITEM))
        val itemWithout = without.cellWidth(without.indexOf(GridColumnKey.ITEM))
        assertTrue("ITEM should gain room when BRAND goes away", itemWithout > itemWith)
    }

    // ── Alignment ────────────────────────────────────────────────────────────

    @Test
    fun `money reads right, counts read centred, codes read left`() {
        val g = grid(CustomerReportOptions(showGstColumn = true, showBrand = true, showSalesman = true, showStatus = true))
        fun alignOf(key: GridColumnKey) = g.columns[g.indexOf(key)].align

        assertEquals(GridAlign.RIGHT, alignOf(GridColumnKey.RATE))
        assertEquals(GridAlign.RIGHT, alignOf(GridColumnKey.TAXABLE))
        assertEquals(GridAlign.RIGHT, alignOf(GridColumnKey.GST))
        assertEquals(GridAlign.CENTRE, alignOf(GridColumnKey.SR))
        assertEquals(GridAlign.CENTRE, alignOf(GridColumnKey.PCS))
        assertEquals(GridAlign.LEFT, alignOf(GridColumnKey.ORDER_NO))
        assertEquals(GridAlign.LEFT, alignOf(GridColumnKey.ITEM))
    }

    /** Whatever the alignment, text has to start and end inside its own cell. */
    @Test
    fun `aligned text stays inside its cell`() {
        val g = grid(CustomerReportOptions(showGstColumn = true))
        g.columns.indices.forEach { index ->
            val room = g.cellWidth(index)
            listOf(0f, room / 2f, room).forEach { textWidth ->
                val x = g.textX(index, textWidth)
                assertTrue(
                    "${g.columns[index].key} text starts before its cell",
                    x >= g.cellLeft(index) - 0.01f
                )
                assertTrue(
                    "${g.columns[index].key} text ends past its cell",
                    x + textWidth <= g.cellRight(index) + 0.01f
                )
            }
        }
    }

    @Test
    fun `right aligned text ends at the cell edge and left aligned starts at it`() {
        val g = grid(CustomerReportOptions(showGstColumn = true))
        val taxable = g.indexOf(GridColumnKey.TAXABLE)
        assertEquals(g.cellRight(taxable) - 20f, g.textX(taxable, 20f), 0.01f)

        val item = g.indexOf(GridColumnKey.ITEM)
        assertEquals(g.cellLeft(item), g.textX(item, 20f), 0.01f)
    }

    // ── Clipping ─────────────────────────────────────────────────────────────

    /** A fixed-width font stands in for a Paint: five points a character, so the maths is checkable. */
    private val measure: (String) -> Float = { it.length * 5f }

    @Test
    fun `text that fits is left exactly as it was`() {
        assertEquals("Shree Balaji", QuotationGrid.clipToWidth("Shree Balaji", 200f, measure))
    }

    @Test
    fun `text that does not fit is cut and marked`() {
        // 100pt of room holds 20 characters
        val clipped = QuotationGrid.clipToWidth("Shree Balaji Fashion And Readymade Garments", 100f, measure)
        assertTrue("a clipped value must say so", clipped.endsWith(".."))
        assertTrue("clipped text must fit the room it was given", measure(clipped) <= 100f)
        assertTrue(clipped.startsWith("Shree Balaji"))
    }

    @Test
    fun `clipping never returns more than it was asked to fit`() {
        val long = "Packed 6 pcs into Mixed Case #2 along with 18 pcs of COT-SHIRT-220 for Ahilyanagar"
        listOf(12f, 40f, 97f, 300f, 1000f).forEach { room ->
            val clipped = QuotationGrid.clipToWidth(long, room, measure)
            assertTrue("clipping to ${room}pt produced ${measure(clipped)}pt", measure(clipped) <= room)
        }
    }

    @Test
    fun `no room means nothing is drawn rather than a stray marker`() {
        assertEquals("", QuotationGrid.clipToWidth("Anything", 0f, measure))
        assertEquals("", QuotationGrid.clipToWidth("Anything", -14f, measure))
        assertEquals("", QuotationGrid.clipToWidth("", 100f, measure))
    }

    /** With room for barely two characters, ".." alone would say nothing — a letter is kept instead. */
    @Test
    fun `a very narrow cell keeps a character rather than only dots`() {
        val clipped = QuotationGrid.clipToWidth("Dispatched", 11f, measure)
        assertFalse("'..' on its own tells the reader nothing", clipped == "..")
        assertTrue(clipped.isNotEmpty())
        assertTrue(measure(clipped) <= 11f)
    }

    // ── The options themselves ───────────────────────────────────────────────

    @Test
    fun `the default layout is the one people already receive`() {
        assertEquals(ReportPdfLayout.MODERN, CustomerReportOptions().layout)
    }

    @Test
    fun `both layouts are offered with their own description`() {
        assertEquals(2, ReportPdfLayout.entries.size)
        ReportPdfLayout.entries.forEach { layout ->
            assertTrue("${layout.name} has no label", layout.label.isNotBlank())
            assertTrue("${layout.name} has no hint", layout.hint.isNotBlank())
        }
        assertNotEquals(ReportPdfLayout.MODERN.label, ReportPdfLayout.RULED_FORM.label)
    }

    @Test
    fun `choosing a layout changes nothing else about the document`() {
        val card = CustomerReportOptions(showGstColumn = true, customNote = "Rate is for this trip only")
        val ruled = card.copy(layout = ReportPdfLayout.RULED_FORM)
        assertEquals(card.showGstColumn, ruled.showGstColumn)
        assertEquals(card.customNote, ruled.customNote)
        // The same toggles produce the same grid whichever layout is named
        assertEquals(QuotationGrid.columnsFor(card).size, QuotationGrid.columnsFor(ruled).size)
    }

    @Test
    fun `half typed extra rows are dropped instead of printed empty`() {
        val options = CustomerReportOptions(
            customFields = listOf(
                ReportCustomField("Broker", "Suresh Bhai"),
                ReportCustomField("", "no label"),
                ReportCustomField("no value", ""),
                ReportCustomField("   ", "   "),
                ReportCustomField("Discount", "2%")
            )
        )
        val usable = options.usableCustomFields()
        assertEquals(2, usable.size)
        assertEquals(listOf("Broker", "Discount"), usable.map { it.label })
    }
}
