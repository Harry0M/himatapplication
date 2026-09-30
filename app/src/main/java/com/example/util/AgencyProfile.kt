package com.example.util

import android.content.Context

/**
 * The agency's own details: how to reach Himat Textile, and the links printed on every document.
 *
 * These used to be hard-coded strings in the PDF footer, which is why the social line read
 * "Instagram • Facebook • LinkedIn • YouTube" with nothing behind it — the words were there but no
 * addresses. Now one admin fills them in once and every phone prints the same footer.
 *
 * Where it lives: the office copy in the RTDB `settings/agency` node is the truth, and each device
 * keeps a local copy in preferences. The PDF writer is a plain function with no access to the app's
 * data flows, so it reads the local copy — which means a report still prints the right footer offline.
 */
data class AgencyProfile(
    val businessName: String = DEFAULT_NAME,
    val tagline: String = DEFAULT_TAGLINE,
    val website: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val instagram: String = "",
    val facebook: String = "",
    val linkedin: String = "",
    val youtube: String = "",
    val upiId: String = ""
) {

    /**
     * The right-hand footer line of every PDF: the website, then whichever social handles are filled
     * in. A platform with no address is left out rather than printed as a dead word.
     */
    fun pdfFooterLine(): String {
        val socials = buildList {
            if (instagram.isNotBlank()) add("Instagram")
            if (facebook.isNotBlank()) add("Facebook")
            if (linkedin.isNotBlank()) add("LinkedIn")
            if (youtube.isNotBlank()) add("YouTube")
        }
        val left = website.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        return when {
            left.isNotBlank() && socials.isNotEmpty() -> "$left   |   ${socials.joinToString(" • ")}"
            left.isNotBlank() -> left
            socials.isNotEmpty() -> socials.joinToString(" • ")
            else -> ""
        }
    }

    /** Every link that is actually filled in, as label to address. Used by the business card. */
    fun filledLinks(): List<Pair<String, String>> = buildList {
        if (website.isNotBlank()) add("Website" to normalise(website))
        if (instagram.isNotBlank()) add("Instagram" to normalise(instagram))
        if (facebook.isNotBlank()) add("Facebook" to normalise(facebook))
        if (linkedin.isNotBlank()) add("LinkedIn" to normalise(linkedin))
        if (youtube.isNotBlank()) add("YouTube" to normalise(youtube))
    }

    private fun normalise(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return value
        return if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
    }

    companion object {
        const val DEFAULT_NAME = "Himat Textile"
        const val DEFAULT_TAGLINE = "Your Garment Guide Across India"

        private const val PREFS = "himat_agency_profile"

        /** The office node this mirrors. Owner-only to write, so branding cannot drift per device. */
        const val NODE = "settings"
        const val CHILD = "agency"

        fun load(context: Context): AgencyProfile {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return AgencyProfile(
                businessName = p.getString("businessName", DEFAULT_NAME) ?: DEFAULT_NAME,
                tagline = p.getString("tagline", DEFAULT_TAGLINE) ?: DEFAULT_TAGLINE,
                website = p.getString("website", "").orEmpty(),
                phone = p.getString("phone", "").orEmpty(),
                whatsapp = p.getString("whatsapp", "").orEmpty(),
                email = p.getString("email", "").orEmpty(),
                address = p.getString("address", "").orEmpty(),
                gstin = p.getString("gstin", "").orEmpty(),
                instagram = p.getString("instagram", "").orEmpty(),
                facebook = p.getString("facebook", "").orEmpty(),
                linkedin = p.getString("linkedin", "").orEmpty(),
                youtube = p.getString("youtube", "").orEmpty(),
                upiId = p.getString("upiId", "").orEmpty()
            )
        }

        fun cache(context: Context, profile: AgencyProfile) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("businessName", profile.businessName)
                .putString("tagline", profile.tagline)
                .putString("website", profile.website)
                .putString("phone", profile.phone)
                .putString("whatsapp", profile.whatsapp)
                .putString("email", profile.email)
                .putString("address", profile.address)
                .putString("gstin", profile.gstin)
                .putString("instagram", profile.instagram)
                .putString("facebook", profile.facebook)
                .putString("linkedin", profile.linkedin)
                .putString("youtube", profile.youtube)
                .putString("upiId", profile.upiId)
                .apply()
        }
    }
}

/**
 * Which parts of the card to include when sharing it.
 *
 * Everything is on by default except the GST number, because a card is usually sent to someone who
 * does not need it yet, and it is the one field people ask to leave out.
 */
data class BusinessCardFields(
    val tagline: Boolean = true,
    val phone: Boolean = true,
    val whatsapp: Boolean = true,
    val email: Boolean = true,
    val address: Boolean = true,
    val gstin: Boolean = false,
    val website: Boolean = true,
    val socials: Boolean = true,
    val upi: Boolean = false,
    val senderName: Boolean = true
)

/**
 * The agency's digital visiting card, as a WhatsApp message.
 *
 * Built from whatever is filled in and whatever the sender ticked: a line with no value is dropped
 * rather than sent as an empty label, so a half-filled profile still produces a card that reads well.
 */
object BusinessCard {

    fun buildMessage(
        profile: AgencyProfile,
        fields: BusinessCardFields = BusinessCardFields(),
        senderName: String = "",
        senderRole: String = ""
    ): String = buildString {
        appendLine("*${profile.businessName.ifBlank { AgencyProfile.DEFAULT_NAME }}*")
        if (fields.tagline && profile.tagline.isNotBlank()) appendLine("_${profile.tagline}_")
        appendLine("━━━━━━━━━━━━━━━━━━━━")

        if (fields.phone && profile.phone.isNotBlank()) appendLine("📞 ${profile.phone}")
        if (fields.whatsapp && profile.whatsapp.isNotBlank() &&
            profile.whatsapp.trim() != profile.phone.trim()
        ) {
            appendLine("💬 WhatsApp: ${profile.whatsapp}")
        }
        if (fields.email && profile.email.isNotBlank()) appendLine("✉️ ${profile.email}")
        if (fields.address && profile.address.isNotBlank()) appendLine("📍 ${profile.address}")
        if (fields.gstin && profile.gstin.isNotBlank()) appendLine("🏛️ GSTIN: ${profile.gstin}")

        val links = profile.filledLinks().filter { (label, _) ->
            if (label == "Website") fields.website else fields.socials
        }
        if (links.isNotEmpty()) {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            links.forEach { (label, url) -> appendLine("$label: $url") }
        }

        if (fields.upi && profile.upiId.isNotBlank()) {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("💳 UPI: ${profile.upiId}")
        }

        if (fields.senderName && senderName.isNotBlank()) {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine(if (senderRole.isNotBlank()) "— $senderName, $senderRole" else "— $senderName")
        }
    }.trim()
}
