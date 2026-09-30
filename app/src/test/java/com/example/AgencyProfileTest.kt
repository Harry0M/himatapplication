package com.example

import com.example.util.AgencyProfile
import com.example.util.BusinessCard
import com.example.util.BusinessCardFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The PDF footer and the business card are both built from whatever happens to be filled in, so the
 * thing worth testing is what they do with the blanks: a platform with no address must not be printed
 * as a dead word, which is exactly what the old hard-coded footer did.
 */
class AgencyProfileTest {

    private val full = AgencyProfile(
        businessName = "Himat Textile",
        tagline = "Your Garment Guide Across India",
        website = "https://himattextile.com",
        phone = "+91 98250 11223",
        whatsapp = "+91 98250 11223",
        email = "himattextile@gmail.com",
        address = "New Cloth Market, Ahmedabad",
        gstin = "24AABCH1234A1Z5",
        instagram = "instagram.com/himattextile",
        facebook = "facebook.com/himattextile",
        linkedin = "",
        youtube = "",
        upiId = "eazypay.0000053310@icici"
    )

    // -------------------------------------------------------------------------
    // PDF footer
    // -------------------------------------------------------------------------

    @Test
    fun footerListsOnlyThePlatformsThatHaveAnAddress() {
        val footer = full.pdfFooterLine()

        assertEquals("himattextile.com   |   Instagram • Facebook", footer)
        assertFalse("LinkedIn has no address, so it must not be printed", footer.contains("LinkedIn"))
        assertFalse(footer.contains("YouTube"))
    }

    @Test
    fun footerStripsTheSchemeAndTrailingSlash() {
        val footer = AgencyProfile(website = "https://himattextile.com/").pdfFooterLine()

        assertEquals("himattextile.com", footer)
    }

    @Test
    fun footerWithNothingFilledInIsEmptyRatherThanDecorative() {
        assertEquals("", AgencyProfile().pdfFooterLine())
    }

    @Test
    fun footerWithSocialsButNoWebsite() {
        val footer = AgencyProfile(instagram = "instagram.com/x").pdfFooterLine()

        assertEquals("Instagram", footer)
    }

    // -------------------------------------------------------------------------
    // Links
    // -------------------------------------------------------------------------

    @Test
    fun linksGetASchemeSoTheyAreTappable() {
        val links = full.filledLinks().toMap()

        assertEquals("https://instagram.com/himattextile", links["Instagram"])
        assertEquals("https://himattextile.com", links["Website"])
    }

    @Test
    fun blankLinksAreLeftOut() {
        val labels = full.filledLinks().map { it.first }

        assertEquals(listOf("Website", "Instagram", "Facebook"), labels)
    }

    // -------------------------------------------------------------------------
    // Business card
    // -------------------------------------------------------------------------

    @Test
    fun cardIncludesWhatWasTickedAndNothingElse() {
        val card = BusinessCard.buildMessage(
            profile = full,
            fields = BusinessCardFields(gstin = false, upi = false),
            senderName = "Ramesh",
            senderRole = "Staff"
        )

        assertTrue(card.contains("Himat Textile"))
        assertTrue(card.contains("+91 98250 11223"))
        assertTrue(card.contains("himattextile@gmail.com"))
        assertTrue(card.contains("New Cloth Market"))
        assertTrue(card.contains("— Ramesh, Staff"))
        assertFalse("GST was not ticked", card.contains("24AABCH1234A1Z5"))
        assertFalse("UPI was not ticked", card.contains("eazypay"))
    }

    @Test
    fun untickingAFieldRemovesIt() {
        val card = BusinessCard.buildMessage(
            profile = full,
            fields = BusinessCardFields(phone = false, address = false, socials = false),
            senderName = "Ramesh"
        )

        assertFalse(card.contains("+91 98250 11223"))
        assertFalse(card.contains("New Cloth Market"))
        assertFalse(card.contains("Instagram"))
        assertTrue("Website is a separate tick from the socials", card.contains("himattextile.com"))
    }

    @Test
    fun whatsappIsNotRepeatedWhenItIsTheSameNumberAsThePhone() {
        val card = BusinessCard.buildMessage(profile = full, senderName = "Ramesh")

        assertEquals(1, card.split("+91 98250 11223").size - 1)
    }

    @Test
    fun aHalfFilledProfileStillProducesAReadableCard() {
        val card = BusinessCard.buildMessage(
            profile = AgencyProfile(businessName = "Himat Textile", phone = "+91 98250 11223"),
            senderName = ""
        )

        assertTrue(card.startsWith("*Himat Textile*"))
        assertTrue(card.contains("+91 98250 11223"))
        assertFalse("No empty label lines", card.contains("✉️ \n"))
        assertFalse(card.contains("📍 \n"))
    }

    @Test
    fun gstAppearsOnlyWhenAskedFor() {
        val card = BusinessCard.buildMessage(profile = full, fields = BusinessCardFields(gstin = true))

        assertTrue(card.contains("GSTIN: 24AABCH1234A1Z5"))
    }
}
