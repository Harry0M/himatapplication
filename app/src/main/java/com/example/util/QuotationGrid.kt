package com.example.util

import com.example.ui.components.CustomerReportOptions

/** Where text sits inside its cell. Amounts read right, codes read left, counts read centred. */
enum class GridAlign { LEFT, CENTRE, RIGHT }

/**
 * A column of the ruled quotation grid, named rather than matched on its caption — the caption is
 * what a customer reads and is free to change, the key is what the code depends on.
 */
enum class GridColumnKey { SR, ORDER_NO, BRAND, ITEM, PCS, RATE, TAXABLE, GST, PACK, DATE, SALESMAN, STATUS }

data class GridColumn(
    val key: GridColumnKey,
    val title: String,
    val weight: Float,
    val align: GridAlign
)

/**
 * The geometry of the ruled (GST / e-way bill style) quotation table.
 *
 * The grid is not a fixed set of columns: the sender switches Brand, Salesman, Status and GST on and
 * off, so the widths, the cell edges and the position of the total figures all move with that choice.
 * That arithmetic is the one part of a hand-drawn PDF that fails silently — a column half a point
 * wide, or a total printed under the wrong heading — and a PDF cannot be rendered in a JVM test, so
 * it lives here as plain numbers that can be checked instead of inside the drawing code.
 */
class QuotationGrid(
    val columns: List<GridColumn>,
    val left: Float,
    val right: Float
) {
    /** Cell boundaries, `columns.size + 1` of them: `edges[i]`..`edges[i + 1]` is column `i`. */
    val edges: FloatArray = FloatArray(columns.size + 1).also { out ->
        val weightSum = columns.fold(0f) { acc, c -> acc + c.weight }
        out[0] = left
        var running = 0f
        columns.forEachIndexed { index, column ->
            running += column.weight
            out[index + 1] = if (weightSum <= 0f) right else left + (running / weightSum) * (right - left)
        }
        if (columns.isNotEmpty()) out[columns.size] = right
    }

    /** -1 when that column was switched off, which callers must treat as "nothing to draw". */
    fun indexOf(key: GridColumnKey): Int = columns.indexOfFirst { it.key == key }

    fun cellLeft(index: Int): Float = edges[index] + PADDING
    fun cellRight(index: Int): Float = edges[index + 1] - PADDING
    fun cellWidth(index: Int): Float = cellRight(index) - cellLeft(index)

    /** The x a string of [textWidth] starts at, honouring the column's alignment. */
    fun textX(index: Int, textWidth: Float): Float = when (columns[index].align) {
        GridAlign.LEFT -> cellLeft(index)
        GridAlign.CENTRE -> cellLeft(index) + ((cellWidth(index) - textWidth) / 2f)
        GridAlign.RIGHT -> cellRight(index) - textWidth
    }

    companion object {
        /** Breathing room on each side of a cell's text, so values never touch the ruled line. */
        const val PADDING = 2.5f

        /**
         * The columns the sender asked for. The eight that are always present are the ones a
         * quotation is not a quotation without; the other four are theirs to drop.
         */
        fun columnsFor(options: CustomerReportOptions): List<GridColumn> = buildList {
            add(GridColumn(GridColumnKey.SR, "SR", 0.5f, GridAlign.CENTRE))
            add(GridColumn(GridColumnKey.ORDER_NO, "ORDER NO.", 1.15f, GridAlign.LEFT))
            if (options.showBrand) {
                add(GridColumn(GridColumnKey.BRAND, "BRAND / MFR", 1.2f, GridAlign.LEFT))
            }
            add(GridColumn(GridColumnKey.ITEM, "ITEM / STYLE", 1.35f, GridAlign.LEFT))
            add(GridColumn(GridColumnKey.PCS, "PCS", 0.55f, GridAlign.CENTRE))
            add(GridColumn(GridColumnKey.RATE, "RATE", 0.85f, GridAlign.RIGHT))
            add(GridColumn(GridColumnKey.TAXABLE, "TAXABLE", 1.05f, GridAlign.RIGHT))
            if (options.showGstColumn) {
                add(GridColumn(GridColumnKey.GST, "GST", 0.85f, GridAlign.RIGHT))
            }
            add(GridColumn(GridColumnKey.PACK, "CASE / PACK", 0.95f, GridAlign.CENTRE))
            add(GridColumn(GridColumnKey.DATE, "DATE", 0.95f, GridAlign.CENTRE))
            if (options.showSalesman) {
                add(GridColumn(GridColumnKey.SALESMAN, "SALESMAN", 1.0f, GridAlign.LEFT))
            }
            if (options.showStatus) {
                add(GridColumn(GridColumnKey.STATUS, "STATUS", 0.95f, GridAlign.CENTRE))
            }
        }

        fun forOptions(options: CustomerReportOptions, left: Float, right: Float): QuotationGrid =
            QuotationGrid(columnsFor(options), left, right)

        /**
         * Shortens [text] until it fits [room], ending in ".." when something was dropped.
         *
         * Takes the measuring function rather than a Paint so the rule can be checked without a
         * drawing surface: the same trimming decides whether a firm name, an address or a note is
         * readable or runs over the line next to it.
         */
        fun clipToWidth(text: String, room: Float, measure: (String) -> Float): String {
            if (text.isEmpty() || room <= 0f) return ""
            if (measure(text) <= room) return text
            var shown = text
            while (shown.length > 1 && measure(shown) > room) shown = shown.dropLast(1)
            return if (shown.length > 2) shown.dropLast(2) + ".." else shown
        }
    }
}
