package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.ui.components.CustomerReportOptions
import com.example.ui.components.ReportOptionsBottomSheet
import com.example.ui.components.ReportPdfLayout
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ReportFields
import com.example.util.ReportOverrides
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The options sheet is where the layout choice is actually made, so it is worth running rather than
 * only compiling: a Compose row that builds fine can still fail the moment it is laid out, and the
 * one thing that must not break is that picking a layout reports the layout that was picked.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ReportOptionsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Held as Compose state, not a plain field: the sheet is told its options from outside, so the
     * test has to hand them over the same way the screen does or nothing on it would ever redraw.
     */
    private var optionsState: MutableState<CustomerReportOptions> = mutableStateOf(CustomerReportOptions())
    private val options: CustomerReportOptions get() = optionsState.value
    private var generated = 0

    /** Stands in for a real trip's resolved values, which the screen passes in for the placeholders. */
    private val recordDefaults = ReportFields(
        firmName = "Shree Balaji Fashion",
        proprietor = "Jitendra Bhai",
        phone = "+91 98765 43210",
        gstin = "27AABCS1429B1ZX",
        address = "Shop 14, Cloth Market",
        cityState = "Ahilyanagar, Maharashtra",
        transporter = "Shree Maruti Transport",
        bookingStation = "Ahmedabad (ADI)",
        lrNo = "MRT-88213",
        dispatchDate = "2026-09-28",
        deliveryTo = "Ahilyanagar, Maharashtra"
    )

    private fun showSheet(initial: CustomerReportOptions = CustomerReportOptions()) {
        optionsState = mutableStateOf(initial)
        generated = 0
        val state = optionsState
        composeTestRule.setContent {
            MyApplicationTheme {
                ReportOptionsBottomSheet(
                    options = state.value,
                    onOptionsChange = { state.value = it },
                    onGeneratePdf = { generated++ },
                    onDismiss = {},
                    defaults = recordDefaults
                )
            }
        }
    }

    @Test
    fun `both layouts are offered on the sheet`() {
        showSheet()
        composeTestRule.onNodeWithText(ReportPdfLayout.MODERN.label).assertIsDisplayed()
        composeTestRule.onNodeWithText(ReportPdfLayout.RULED_FORM.label).assertIsDisplayed()
    }

    @Test
    fun `picking the GST form reports that layout`() {
        showSheet()
        assertEquals(ReportPdfLayout.MODERN, options.layout)

        composeTestRule.onNodeWithText(ReportPdfLayout.RULED_FORM.label).performClick()
        composeTestRule.waitForIdle()

        assertEquals(ReportPdfLayout.RULED_FORM, options.layout)
    }

    /** Choosing a layout must not quietly reset the toggles the sender already set. */
    @Test
    fun `picking a layout leaves the other choices alone`() {
        showSheet(
            CustomerReportOptions(
                showGstColumn = true,
                showStatus = false,
                customNote = "Rate is for this trip only"
            )
        )
        composeTestRule.onNodeWithText(ReportPdfLayout.RULED_FORM.label).performClick()
        composeTestRule.waitForIdle()

        assertEquals(ReportPdfLayout.RULED_FORM, options.layout)
        assertEquals(true, options.showGstColumn)
        assertEquals(false, options.showStatus)
        assertEquals("Rate is for this trip only", options.customNote)
    }

    /** The button says which document is about to come out, so there is no surprise after sharing. */
    @Test
    fun `the generate button names the chosen layout`() {
        showSheet()
        composeTestRule.onNodeWithText("Generate ${ReportPdfLayout.MODERN.label} PDF").assertIsDisplayed()

        composeTestRule.onNodeWithText(ReportPdfLayout.RULED_FORM.label).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Generate ${ReportPdfLayout.RULED_FORM.label} PDF").assertIsDisplayed()
    }

    @Test
    fun `the generate button asks for the pdf`() {
        showSheet()
        composeTestRule.onNodeWithText("Generate ${ReportPdfLayout.MODERN.label} PDF").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, generated)
    }

    /** The sheet still carries the fields it had before the layout chooser was added on top. */
    @Test
    fun `the existing toggles are still on the sheet`() {
        showSheet()
        listOf("Transport section", "Phone number", "GSTIN", "Brand", "Salesman")
            .forEach { label ->
                composeTestRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            }
    }

    @Test
    fun `a toggle still reports its change`() {
        showSheet()
        composeTestRule.onNodeWithText("GST amount").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, options.showGstColumn)
    }

    /** Nothing is generated just by opening the sheet. */
    @Test
    fun `opening the sheet generates nothing`() {
        showSheet()
        composeTestRule.waitForIdle()
        assertEquals(0, generated)
        assertNull(options.customFields.firstOrNull())
    }

    @Test
    fun `the sheet explains what each layout is for`() {
        showSheet()
        composeTestRule.onNode(hasText(ReportPdfLayout.RULED_FORM.hint, substring = true)).assertIsDisplayed()
    }

    // ── Correcting a value for this one PDF ──────────────────────────────────

    /** Typing over the transporter has to reach the options, which is what the PDF writer reads. */
    @Test
    fun `typing over the transporter reports the new value`() {
        showSheet()
        composeTestRule.onAllNodesWithText("Edit Transporter name")
            .filterToOne(hasSetTextAction())
            .performTextReplacement("V.R.L. Logistics")
        composeTestRule.waitForIdle()

        assertEquals("V.R.L. Logistics", options.overrides.transporter)
    }

    @Test
    fun `every transport and customer field can be typed over`() {
        showSheet()
        mapOf(
            "Edit Transporter name" to "V.R.L. Logistics",
            "Edit LR / booking number" to "VRL-99881",
            "Edit Firm / shop name" to "Balaji Readymade",
            "Edit GSTIN" to "27ZZZZZ9999Z1ZZ"
        ).forEach { (field, typed) ->
            composeTestRule.onAllNodesWithText(field).filterToOne(hasSetTextAction()).performTextReplacement(typed)
            composeTestRule.waitForIdle()
        }

        assertEquals("V.R.L. Logistics", options.overrides.transporter)
        assertEquals("VRL-99881", options.overrides.lrNo)
        assertEquals("Balaji Readymade", options.overrides.firmName)
        assertEquals("27ZZZZZ9999Z1ZZ", options.overrides.gstin)
        assertEquals(4, options.overrides.editedCount)
    }

    /**
     * What the record says has to be readable without tapping the box. Material hides a placeholder
     * until the field is focused, so this is carried in the supporting line instead.
     */
    @Test
    fun `the box says what the record currently holds without being tapped`() {
        showSheet()
        composeTestRule.onNode(hasText("On record: ${recordDefaults.transporter}", substring = true))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("On record: ${recordDefaults.lrNo}", substring = true))
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** Once edited, the line says what is being replaced rather than what will print. */
    @Test
    fun `an edited box says what it is replacing`() {
        showSheet(CustomerReportOptions(overrides = ReportOverrides(transporter = "V.R.L. Logistics")))
        composeTestRule.onNode(hasText("Replacing: ${recordDefaults.transporter}", substring = true))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `an edit is announced with a count and can be undone`() {
        showSheet(CustomerReportOptions(overrides = ReportOverrides(transporter = "V.R.L. Logistics")))
        composeTestRule.onNode(hasText("1 field(s) edited", substring = true)).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Undo all").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue("undo should put every field back", options.overrides.isEmpty)
    }

    /** Switching a field off removes its box: there is nothing to correct on a field not printed. */
    @Test
    fun `a switched off field offers no box`() {
        showSheet()
        composeTestRule.onAllNodesWithText("Edit Phone number").filterToOne(hasSetTextAction())

        composeTestRule.onNodeWithText("Phone number").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(false, options.showPhone)
        composeTestRule.onAllNodesWithText("Edit Phone number").assertCountEquals(0)
    }

    /** Editing the document must never be mistaken for editing the customer master. */
    @Test
    fun `the sheet says the customer record is not changed`() {
        showSheet()
        composeTestRule.onNode(hasText("customer master is not changed", substring = true))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
