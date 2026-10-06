package com.animalvision.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.SourceType

/**
 * Full-screen scientific info panel (spec §21): color/acuity/FOV/low-light facts,
 * confidence bars, limitations, sources, and profile provenance.
 */
@Composable
fun AnimalInfoScreen(
    profile: AnimalVisionProfile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = AvInk,
    ) {
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
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AvTextPrimary,
                    )
                    Text(
                        text = profile.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvMuted,
                        fontStyle = FontStyle.Italic,
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyLarge,
                color = AvTextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )

            SectionCard(title = "Color vision") {
                Text(
                    text = "${profile.colorVision.photoreceptorCount} photoreceptor classes: " +
                        profile.colorVision.photoreceptorTypes.joinToString(", ") +
                        ". Discrimination axes: " +
                        profile.colorVision.discriminationAxes.joinToString(", ") + ".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvTextPrimary,
                )
                Text(
                    text = profile.colorVision.sensitivities.joinToString(" · ") {
                        "${it.id} peak ${it.peakNm.toInt()} nm"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AvMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = profile.colorVision.lutNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = AvMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SectionCard(title = "Acuity") {
                Text(
                    text = "Detail limit ${profile.acuity.cutoffCpd} cycles/degree " +
                        "(${(profile.acuity.relativeAcuity * 100).toInt()}% of a nominal human reference). " +
                        "Central region ×${profile.acuity.centralScalar}, " +
                        "periphery ×${profile.acuity.peripheralScalar}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvTextPrimary,
                )
            }

            SectionCard(title = "Field of view") {
                Text(
                    text = "${profile.fieldOfView.totalHorizontalDeg.toInt()}° total horizontal, " +
                        "${profile.fieldOfView.binocularWidthDeg.toInt()}° binocular overlap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvTextPrimary,
                )
                if (profile.fieldOfView.blindSpotLabels.isNotEmpty()) {
                    Text(
                        text = "Blind regions: " + profile.fieldOfView.blindSpotLabels.joinToString("; "),
                        style = MaterialTheme.typography.bodySmall,
                        color = AvMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = profile.fieldOfView.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = AvMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SectionCard(title = "Low light") {
                if (profile.lowLight.enabled) {
                    Text(
                        text = "Relative light sensitivity ≈ ${profile.lowLight.sensitivity}× human. " +
                            "Simulation lifts shadows, reduces saturation toward rod vision, " +
                            "and adds bounded noise — never a plain brightness boost.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvTextPrimary,
                    )
                } else {
                    Text(
                        text = "No special low-light model for this species.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvMuted,
                    )
                }
            }

            SectionCard(title = "Motion & timing") {
                Text(
                    text = profile.motion.temporalResolutionHz?.let {
                        "Flicker fusion ≈ ${it.toInt()} Hz; motion sensitivity ×${profile.motion.motionSensitivity}."
                    } ?: "Flicker fusion frequency unknown for this species.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvTextPrimary,
                )
            }

            val specialLines = buildList {
                profile.specialVision.ultraviolet?.let { add("Ultraviolet: ${it.falseColorLabel}") }
                profile.specialVision.infrared?.let { add("Infrared: ${it.label}") }
                addAll(profile.specialVision.notes)
            }
            if (specialLines.isNotEmpty()) {
                SectionCard(title = "Special channels (simulations)") {
                    specialLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvWarning,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            SectionCard(title = "Scientific confidence") {
                val c = profile.scientificData.confidence
                ConfidenceBar(label = "Color", confidence = c.color)
                Spacer(Modifier.height(6.dp))
                ConfidenceBar(label = "Acuity", confidence = c.acuity)
                Spacer(Modifier.height(6.dp))
                ConfidenceBar(label = "Field of view", confidence = c.fov)
                Spacer(Modifier.height(6.dp))
                ConfidenceBar(label = "Low light", confidence = c.lowLight)
                Spacer(Modifier.height(6.dp))
                ConfidenceBar(label = "Overall", confidence = c.overall)
            }

            SectionCard(title = "Limitations") {
                profile.scientificData.limitations.forEach { limitation ->
                    Text(
                        text = "• $limitation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvTextPrimary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SectionCard(title = "Sources") {
                profile.scientificData.sources.forEach { src ->
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            text = src.cite,
                            style = MaterialTheme.typography.bodySmall,
                            color = AvTextPrimary,
                        )
                        val meta = listOfNotNull(
                            src.doi?.let { "DOI $it" },
                            when (src.type) {
                                SourceType.MEASURED -> "measured"
                                SourceType.INFERRED -> "inferred"
                                SourceType.MODELED -> "modeled"
                            },
                        ).joinToString(" · ")
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = AvMuted,
                        )
                    }
                }
            }

            Text(
                text = "Profile ${profile.profileVersion} · research updated ${profile.researchUpdated}",
                style = MaterialTheme.typography.labelSmall,
                color = AvMuted,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .semantics {
                        contentDescription =
                            "Profile ${profile.profileVersion}, research updated ${profile.researchUpdated}"
                    },
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AvPanel)
            .padding(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AvAccent,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

