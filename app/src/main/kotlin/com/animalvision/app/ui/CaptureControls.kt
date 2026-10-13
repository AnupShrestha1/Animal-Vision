package com.animalvision.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.animalvision.app.DisplayMode
import com.animalvision.app.LastCapture

/**
 * Camera-style capture row (spec §29): last-capture thumbnail, a large photo shutter, and a
 * record toggle. [vertical] stacks them along the right edge for landscape.
 */
@Composable
fun ShutterRow(
    lastCapture: LastCapture?,
    onOpenLastCapture: () -> Unit,
    onCapturePhoto: () -> Unit,
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val thumb = @Composable { LastCaptureThumb(lastCapture, onOpenLastCapture) }
    val shutter = @Composable { Shutter(onCapturePhoto) }
    val record = @Composable { RecordButton(isRecording, onToggleRecord) }
    if (vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            record()
            shutter()
            thumb()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            thumb()
            shutter()
            record()
        }
    }
}

@Composable
private fun Shutter(onCapturePhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onCapturePhoto)
            .padding(8.dp)
            .clip(CircleShape)
            .background(Color.White)
            .semantics {
                role = Role.Button
                contentDescription = "Take photo of the animal view"
            },
    )
}

@Composable
private fun RecordButton(isRecording: Boolean, onToggleRecord: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, if (isRecording) AvDanger else Color.White.copy(alpha = 0.9f), CircleShape)
            .clickable(onClick = onToggleRecord)
            .semantics {
                role = Role.Button
                contentDescription = if (isRecording) "Stop video recording" else "Start video recording"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 18.dp else 22.dp)
                .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                .background(AvDanger),
        )
    }
}

@Composable
private fun LastCaptureThumb(lastCapture: LastCapture?, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(2.dp, Color.White.copy(alpha = if (lastCapture != null) 0.9f else 0.3f), shape)
            .then(
                if (lastCapture != null) {
                    Modifier
                        .clickable(onClick = onOpen)
                        .semantics {
                            role = Role.Button
                            contentDescription =
                                if (lastCapture.isVideo) "Open last video" else "Open last photo"
                        }
                } else {
                    Modifier.semantics { contentDescription = "No captures yet" }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (lastCapture != null) {
            Image(
                bitmap = lastCapture.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (lastCapture.isVideo) {
                Text(
                    text = "▶",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Before/after selector (spec §20): Animal / Split / Human. Selected state is conveyed by
 * fill, weight, and the semantics "selected" flag, not color alone.
 */
@Composable
fun ModeSelector(
    displayMode: Int,
    onSetMode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(
        DisplayMode.ANIMAL to "Animal",
        DisplayMode.SPLIT to "Split",
        DisplayMode.NORMAL to "Human",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        modes.forEach { (mode, label) ->
            val selected = mode == displayMode
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AvInk else Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSetMode(mode) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                        contentDescription = when (mode) {
                            DisplayMode.SPLIT -> "Split view: animal left, human right"
                            DisplayMode.NORMAL -> "Human view, unprocessed camera"
                            else -> "Animal view"
                        }
                    },
            )
        }
    }
}
