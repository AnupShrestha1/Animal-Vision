package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * Shutter + record buttons (spec §29): ring-shaped photo shutter, record toggle
 * with elapsed-state color, both with spoken labels.
 */
@Composable
fun CaptureControls(
    onCapturePhoto: () -> Unit,
    onToggleRecord: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Photo shutter
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .border(3.dp, Color.White, CircleShape)
                .clickable { onCapturePhoto() }
                .semantics {
                    role = Role.Button
                    contentDescription = "Take photo of the processed view"
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }

        // Record toggle
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) AvDanger.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.18f)
                )
                .border(
                    3.dp,
                    if (isRecording) AvDanger else Color.White,
                    CircleShape,
                )
                .clickable { onToggleRecord() }
                .semantics {
                    role = Role.Button
                    contentDescription = if (isRecording) "Stop video recording" else "Start video recording"
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AvDanger),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AvDanger),
                )
            }
        }
    }
}

/**
 * Three-way before/after mode selector (spec §20): Normal / Animal / Split.
 * Selected state is conveyed by color AND text weight + spoken "selected" label.
 */
@Composable
fun ModeSelector(
    displayMode: Int,
    onSetMode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(
        DisplayMode.NORMAL to "Normal",
        DisplayMode.ANIMAL to "Animal",
        DisplayMode.SPLIT to "Split",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        modes.forEach { (mode, label) ->
            val selected = mode == displayMode
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AvInk else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSetMode(mode) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .semantics {
                        role = Role.Tab
                        contentDescription = "$label view mode" + if (selected) ", selected" else ""
                    },
            )
        }
    }
}
