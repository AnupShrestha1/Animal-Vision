package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.animalvision.app.DisplayMode
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.stage.Motion
import com.animalvision.core.stage.SpecialVision

/**
 * Compact educational overlay above the capture controls (spec §22): species fact,
 * simulation labels, and a tap-through hint to the full info panel.
 */
@Composable
fun EducationalOverlay(
    profile: AnimalVisionProfile,
    modifier: Modifier = Modifier,
    onOpenInfo: (() -> Unit)? = null,
) {
    // Every applicable notice is shown: a species can carry several simulated channels
    // (the bee has UV, polarization, and fast temporal vision) and none may be hidden.
    val notes = buildList {
        if (profile.specialVision.ultraviolet != null) {
            add("UV channel is a labeled SIMULATION — phone cameras cannot see ultraviolet.")
        }
        if (profile.specialVision.infrared != null) {
            add("Thermal view is a labeled SIMULATION — an RGB camera cannot detect infrared.")
        }
        if (profile.specialVision.polarizationSimulation) add(SpecialVision.POLARIZATION_DISCLOSURE)
        Motion.disclosure(profile)?.let { add(it) }
        if (profile.lowLight.enabled) {
            add("Low-light: ~${profile.lowLight.sensitivity}× human sensitivity (modeled).")
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .then(if (onOpenInfo != null) Modifier.clickable { onOpenInfo() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${profile.name.uppercase()} · ${profile.scientificName}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = profile.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        notes.forEach { simulationNote ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = simulationNote,
                style = MaterialTheme.typography.labelSmall,
                color = AvWarning,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .semantics { contentDescription = "Simulation notice: $simulationNote" },
            )
        }
        if (onOpenInfo != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap for science & confidence",
                style = MaterialTheme.typography.labelSmall,
                color = AvAccent.copy(alpha = 0.9f),
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "Open scientific info panel for ${profile.name}"
                },
            )
        }
    }
}
