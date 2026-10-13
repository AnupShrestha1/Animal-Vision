package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.animalvision.app.DisplayMode
import com.animalvision.core.model.AnimalVisionProfile

/**
 * Central camera experience (spec §17–18): viewfinder with before/after controls,
 * educational overlay, status line, and capture controls. The GLSurfaceView itself
 * is hosted by the Activity and rendered beneath this overlay layer.
 */
@Composable
fun CameraScreen(
    profile: AnimalVisionProfile,
    displayMode: Int,
    onSetMode: (Int) -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    educationalMode: Boolean,
    capabilityNote: String?,
    statusMessage: String?,
    fps: Double,
    isRecording: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showModeHint by remember { mutableStateOf(false) }
    LaunchedEffect(displayMode) {
        showModeHint = true
        kotlinx.coroutines.delay(1500)
        showModeHint = false
    }

    // Landscape (sensor-driven, see manifest fullSensor) trims the deck so the
    // wide horizontal field — the blind spots landscape exists to show — stays
    // unobstructed; portrait keeps the full educational layout.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(modifier = modifier.fillMaxSize()) {
        // --- press-and-hold viewfinder gesture (spec §20) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onHoldStart()
                            try {
                                awaitRelease()
                            } finally {
                                onHoldEnd()
                            }
                        },
                    )
                },
        )

        // --- split-mode divider label ---
        if (displayMode == DisplayMode.SPLIT) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isLandscape) 56.dp else 96.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "ANIMAL VISION   |   HUMAN VISION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // --- viewfinder HUD: sits BELOW the top bar so nothing overlaps it.
        // Order: capability note first (long chip), then a compact row of fps + REC.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 52.dp, end = 12.dp)
                .widthIn(max = 250.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (capabilityNote != null) {
                Text(
                    text = capabilityNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = AvWarning,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .widthIn(max = 230.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (fps > 0) {
                    Text(
                        text = "${fps.toInt()} fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (isRecording) {
                    Text(
                        text = "▶ REC",
                        style = MaterialTheme.typography.labelLarge,
                        color = AvDanger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .semantics { contentDescription = "Recording in progress" },
                    )
                }
            }
        }

        // --- educational overlay (spec §22) ---
        // Bottom scrim keeps the control deck legible over bright scenes.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isLandscape) 240.dp else 400.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )

        // --- bottom control deck: one column so spacing stays consistent ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (educationalMode && !isLandscape) {
                EducationalOverlay(
                    profile = profile,
                    onOpenInfo = onOpenInfo,
                )
                Spacer(Modifier.height(10.dp))
            }

            // --- transient mode hint (snack-style, above the deck) ---
            if (showModeHint) {
                Text(
                    text = modeLabel(displayMode),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .semantics { contentDescription = modeLabel(displayMode) },
                )
                Spacer(Modifier.height(10.dp))
            }

            // --- status toast ---
            if (statusMessage != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { contentDescription = statusMessage },
                )
                Spacer(Modifier.height(12.dp))
            }

            // --- capture controls ---
            CaptureControls(
                onCapturePhoto = onCapturePhoto,
                onToggleRecord = onToggleRecord,
                isRecording = isRecording,
            )
            Spacer(Modifier.height(14.dp))

            // --- mode selector ---
            ModeSelector(
                displayMode = displayMode,
                onSetMode = onSetMode,
            )

            // Reserved space for the animal selector strip (rendered by the activity
            // above this screen), so the two never collide.
            Spacer(Modifier.height(AnimalSelectorHeight))
        }
    }
}

private fun modeLabel(mode: Int): String = when (mode) {
    DisplayMode.NORMAL -> "Normal human vision"
    DisplayMode.SPLIT -> "Split: animal left · human right"
    else -> "Animal vision"
}

