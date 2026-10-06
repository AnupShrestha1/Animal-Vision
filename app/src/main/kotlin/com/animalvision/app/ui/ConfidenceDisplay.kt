package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalvision.core.model.Confidence

/**
 * Text-based five-block confidence indicator (spec §36: never color/shape alone —
 * filled/empty blocks plus a spoken label like "Acuity confidence: 4 out of 5 stars").
 */
@Composable
fun ConfidenceBar(
    label: String,
    confidence: Confidence,
    modifier: Modifier = Modifier,
) {
    val stars = confidence.stars
    Row(
        modifier = modifier.semantics {
            contentDescription = "$label confidence: $stars out of 5 stars, ${confidence.readableName}"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AvTextPrimary,
            modifier = Modifier.padding(end = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i <= stars) MaterialTheme.colorScheme.primary
                            else AvPanelElevated
                        )
                )
            }
        }
        Text(
            text = confidence.readableName,
            style = MaterialTheme.typography.labelSmall,
            color = AvMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

val Confidence.readableName: String
    get() = when (this) {
        Confidence.WELL_ESTABLISHED -> "well established"
        Confidence.STRONG -> "strong"
        Confidence.REASONABLE -> "reasonable"
        Confidence.UNCERTAIN -> "uncertain"
        Confidence.SPECULATIVE -> "speculative"
    }
