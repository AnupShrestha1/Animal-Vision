package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.animalvision.core.model.QualityTier

/**
 * Settings panel (spec §24): quality tier, educational overlay, low-light stage,
 * privacy statement, about/limitations.
 */
@Composable
fun SettingsScreen(
    qualityTier: QualityTier,
    onQualityTier: (QualityTier) -> Unit,
    educationalMode: Boolean,
    onEducationalMode: (Boolean) -> Unit,
    lowLightEnabled: Boolean,
    onLowLightEnabled: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = AvInk) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AvTextPrimary,
                    modifier = Modifier.padding(top = 16.dp),
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = "Quality",
                style = MaterialTheme.typography.titleMedium,
                color = AvAccent,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Lower tiers reduce internal resolution and stage count if the device struggles.",
                style = MaterialTheme.typography.bodySmall,
                color = AvMuted,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QualityTier.entries.forEach { tier ->
                    val selected = tier == qualityTier
                    Text(
                        text = tier.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) AvInk else AvTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else AvPanel
                            )
                            .clickable { onQualityTier(tier) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Quality ${tier.label}" +
                                    if (selected) ", selected" else ""
                            },
                    )
                }
            }

            ToggleRow(
                title = "Educational overlay",
                subtitle = "Show species facts and simulation labels over the viewfinder.",
                checked = educationalMode,
                onCheckedChange = onEducationalMode,
            )
            ToggleRow(
                title = "Low-light simulation",
                subtitle = "Species shadow lift and rod desaturation. Disable to save battery.",
                checked = lowLightEnabled,
                onCheckedChange = onLowLightEnabled,
            )

            SectionTitle("Privacy")
            Text(
                text = "Camera frames are processed on-device only. Nothing is uploaded; " +
                    "photos and videos stay in your gallery unless you share them.",
                style = MaterialTheme.typography.bodyMedium,
                color = AvTextPrimary,
            )

            SectionTitle("About")
            Text(
                text = "Animal Vision simulates how fourteen animals see — human, dog, cat, " +
                    "horse, eagle, bee, snake, pigeon, chicken, butterfly, fruit fly, " +
                    "octopus, mantis shrimp, and dolphin — from peer-reviewed vision science. " +
                    "Color, acuity, and field-of-view models are labeled with confidence " +
                    "levels; ultraviolet, thermal, and polarization modes are clearly marked " +
                    "simulations. Subjective experience is unobservable: this is a scientific " +
                    "visualization, not a claim about what an animal 'feels'.",
                style = MaterialTheme.typography.bodyMedium,
                color = AvTextPrimary,
            )
            Text(
                text = "No storage permission is required; captures use the system gallery APIs. " +
                    "The microphone is never recorded — video capture is silent by design.",
                style = MaterialTheme.typography.bodySmall,
                color = AvMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = AvAccent,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = AvTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = AvMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics {
                contentDescription = "$title, ${if (checked) "on" else "off"}"
            },
        )
    }
}

