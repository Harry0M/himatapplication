package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InfoCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.AgencyProfile

/**
 * The agency's own details: what is printed at the bottom of every PDF and shared on the business card.
 *
 * Admin only, matching the database rule — this text goes out to every customer, so it is not
 * something a phone should be able to change on its own. A platform left blank is simply not printed
 * rather than appearing as a word with nothing behind it, which is what the old hard-coded footer did.
 */
@Composable
fun SettingsScreen(viewModel: HimatViewModel, onBack: () -> Unit) {
    val saved by viewModel.agencyProfile.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()

    // Re-seeded whenever the office sends a change, so two admins editing do not fight
    var businessName by remember(saved) { mutableStateOf(saved.businessName) }
    var tagline by remember(saved) { mutableStateOf(saved.tagline) }
    var phone by remember(saved) { mutableStateOf(saved.phone) }
    var whatsapp by remember(saved) { mutableStateOf(saved.whatsapp) }
    var email by remember(saved) { mutableStateOf(saved.email) }
    var address by remember(saved) { mutableStateOf(saved.address) }
    var gstin by remember(saved) { mutableStateOf(saved.gstin) }
    var website by remember(saved) { mutableStateOf(saved.website) }
    var instagram by remember(saved) { mutableStateOf(saved.instagram) }
    var facebook by remember(saved) { mutableStateOf(saved.facebook) }
    var linkedin by remember(saved) { mutableStateOf(saved.linkedin) }
    var youtube by remember(saved) { mutableStateOf(saved.youtube) }
    var upiId by remember(saved) { mutableStateOf(saved.upiId) }
    var saving by remember { mutableStateOf(false) }

    val draft = AgencyProfile(
        businessName = businessName.trim().ifBlank { AgencyProfile.DEFAULT_NAME },
        tagline = tagline.trim(),
        website = website.trim(),
        phone = phone.trim(),
        whatsapp = whatsapp.trim(),
        email = email.trim(),
        address = address.trim(),
        gstin = gstin.trim().uppercase(),
        instagram = instagram.trim(),
        facebook = facebook.trim(),
        linkedin = linkedin.trim(),
        youtube = youtube.trim(),
        upiId = upiId.trim()
    )

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Business details",
                    subtitle = "Printed on every PDF and the business card",
                    onBack = onBack
                )
            }

            if (!isAdmin) {
                item {
                    InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                        Text(
                            "Only an Admin can change these details.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "They appear on documents that go to customers, so they are kept in one place for the whole agency.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    Text("How the PDF footer will read", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = draft.pdfFooterLine().ifBlank { "(nothing filled in yet)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "A platform with nothing in it is left out, so the footer never shows a name with no address behind it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            item { Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) { SectionHeader(title = "Business") } }
            item {
                SettingsFields(
                    enabled = isAdmin,
                    fields = listOf(
                        SettingField("Business name", businessName, { businessName = it }),
                        SettingField("Tagline", tagline, { tagline = it }),
                        SettingField("GSTIN", gstin, { gstin = it }),
                        SettingField("Shop / office address", address, { address = it }, singleLine = false)
                    )
                )
            }

            item { Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) { SectionHeader(title = "Contact") } }
            item {
                SettingsFields(
                    enabled = isAdmin,
                    fields = listOf(
                        SettingField("Phone", phone, { phone = it }, keyboard = KeyboardType.Phone),
                        SettingField("WhatsApp (if different)", whatsapp, { whatsapp = it }, keyboard = KeyboardType.Phone),
                        SettingField("Email", email, { email = it }, keyboard = KeyboardType.Email),
                        SettingField("UPI id", upiId, { upiId = it })
                    )
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    SectionHeader(title = "Website and social links")
                    Text(
                        "Paste the full address or just the handle page — the app adds https:// when it shares them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            item {
                SettingsFields(
                    enabled = isAdmin,
                    fields = listOf(
                        SettingField("Website", website, { website = it }, keyboard = KeyboardType.Uri),
                        SettingField("Instagram", instagram, { instagram = it }, keyboard = KeyboardType.Uri),
                        SettingField("Facebook", facebook, { facebook = it }, keyboard = KeyboardType.Uri),
                        SettingField("LinkedIn", linkedin, { linkedin = it }, keyboard = KeyboardType.Uri),
                        SettingField("YouTube", youtube, { youtube = it }, keyboard = KeyboardType.Uri)
                    )
                )
            }

            if (isAdmin) {
                item {
                    PrimaryButton(
                        text = if (saving) "Saving..." else "Save business details",
                        icon = Icons.Default.Save,
                        enabled = !saving,
                        onClick = {
                            saving = true
                            viewModel.saveAgencyProfile(draft) { saving = false }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UiDimens.ScreenPadding, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private data class SettingField(
    val label: String,
    val value: String,
    val onChange: (String) -> Unit,
    val keyboard: KeyboardType = KeyboardType.Text,
    val singleLine: Boolean = true
)

@Composable
private fun SettingsFields(enabled: Boolean, fields: List<SettingField>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        fields.forEach { field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = field.onChange,
                label = { Text(field.label) },
                enabled = enabled,
                singleLine = field.singleLine,
                minLines = if (field.singleLine) 1 else 2,
                keyboardOptions = KeyboardOptions(
                    keyboardType = field.keyboard,
                    imeAction = if (field.singleLine) ImeAction.Next else ImeAction.Default
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
