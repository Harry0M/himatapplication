package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.BusinessCardFields

/**
 * The agency's digital visiting card, on its way to WhatsApp.
 *
 * The tick boxes matter: a card sent to a new supplier is not the same card you send a customer who
 * needs to pay, so the sender decides what goes. What is shown below the boxes is the actual message
 * that will be sent, not a mock-up — the same builder produces both, so there is nothing to drift.
 *
 * A field with nothing behind it is not offered at all, rather than being offered and then silently
 * dropping out of the message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardSheet(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit,
    onEditDetails: (() -> Unit)? = null
) {
    val profile by viewModel.agencyProfile.collectAsStateWithLifecycle()
    var fields by remember { mutableStateOf(BusinessCardFields()) }

    val preview = viewModel.buildBusinessCard(fields)
    val nothingFilledIn = profile.phone.isBlank() &&
        profile.email.isBlank() &&
        profile.website.isBlank() &&
        profile.address.isBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                "Share our business card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tick what should go in the message.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (nothingFilledIn) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "The business details are empty",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "An Admin needs to fill in the phone, address and links once. Until then the card is just the business name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (onEditDetails != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryButton(
                                text = "Open business details",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    onDismiss()
                                    onEditDetails()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Only offer what there is something to send
            CardToggle("Tagline", profile.tagline.isNotBlank(), fields.tagline) { fields = fields.copy(tagline = it) }
            CardToggle("Phone", profile.phone.isNotBlank(), fields.phone) { fields = fields.copy(phone = it) }
            CardToggle(
                label = "WhatsApp number",
                available = profile.whatsapp.isNotBlank() && profile.whatsapp.trim() != profile.phone.trim(),
                checked = fields.whatsapp
            ) { fields = fields.copy(whatsapp = it) }
            CardToggle("Email", profile.email.isNotBlank(), fields.email) { fields = fields.copy(email = it) }
            CardToggle("Address", profile.address.isNotBlank(), fields.address) { fields = fields.copy(address = it) }
            CardToggle("GST number", profile.gstin.isNotBlank(), fields.gstin) { fields = fields.copy(gstin = it) }
            CardToggle("Website", profile.website.isNotBlank(), fields.website) { fields = fields.copy(website = it) }
            CardToggle(
                label = "Social links",
                available = profile.instagram.isNotBlank() || profile.facebook.isNotBlank() ||
                    profile.linkedin.isNotBlank() || profile.youtube.isNotBlank(),
                checked = fields.socials
            ) { fields = fields.copy(socials = it) }
            CardToggle("UPI id", profile.upiId.isNotBlank(), fields.upi) { fields = fields.copy(upi = it) }
            CardToggle("My name at the bottom", true, fields.senderName) { fields = fields.copy(senderName = it) }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "This is what gets sent",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Share on WhatsApp",
                icon = Icons.Default.Share,
                onClick = {
                    viewModel.shareBusinessCard(fields)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * One tick box. [available] false means there is nothing filled in for it, so the row is skipped
 * entirely — offering a switch that changes nothing is worse than not offering it.
 */
@Composable
private fun CardToggle(
    label: String,
    available: Boolean,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    if (!available) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
