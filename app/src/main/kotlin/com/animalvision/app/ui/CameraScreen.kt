package com.animalvision.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animalvision.app.DisplayMode
import com.animalvision.app.LastCapture
import com.animalvision.core.model.AnimalVisionProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** A press shorter than this is a tap (focus); longer is a hold (peek at human vision). */
private const val HOLD_DELAY_MS = 200L

/** Distance from the top safe edge to the split labels: clears the button row and badge row. */
private val SplitLabelTop = 112.dp

/** Width reserved on the right edge for the landscape capture column. */
private val LandscapeControlsWidth = 104.dp

/**
 * Central camera experience (spec §17–18). The GLSurfaceView is hosted by the Activity
 * beneath this overlay; everything drawn here is chrome and must leave the view visible:
 *  - top: current animal (opens info), honesty badges, REC timer, camera/settings buttons;
 *  - bottom (portrait): one-line fact, Animal/Split/Human, animal carousel, capture row;
 *  - landscape: the capture row moves to a column on the right edge.
 * Gestures on the view: tap = focus, press-and-hold = peek at human vision.
 */
@Composable
fun CameraScreen(
    profile: AnimalVisionProfile,
    profiles: List<AnimalVisionProfile>,
    onSelectProfile: (String) -> Unit,
    displayMode: Int,
    onSetMode: (Int) -> Unit,
    splitPosition: Float,
    onSplitChange: (Float) -> Unit,
    zoom: Float,
    maxZoom: Float,
    onZoom: (Float) -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onTapFocus: (x: Float, y: Float) -> Unit,
    educationalMode: Boolean,
    statusMessage: String?,
    showStats: Boolean,
    fps: Double,
    isRecording: Boolean,
    recordingStartedAtMs: Long,
    lastCapture: LastCapture?,
    captureFlashTick: Int,
    onCapturePhoto: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenLastCapture: () -> Unit,
    hasFlash: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(focusTick) {
        if (focusPoint != null) {
            delay(900)
            focusPoint = null
        }
    }

    // Short-lived label naming the mode just chosen.
    var showModeHint by remember { mutableStateOf(false) }
    var firstMode by remember { mutableStateOf(true) }
    LaunchedEffect(displayMode) {
        if (firstMode) {
            firstMode = false
            return@LaunchedEffect
        }
        showModeHint = true
        delay(1300)
        showModeHint = false
    }

    // "Try this" hint for a few seconds whenever the animal changes (not on first show).
    var showTip by remember { mutableStateOf(false) }
    var firstProfile by remember { mutableStateOf(true) }
    LaunchedEffect(profile.id) {
        if (firstProfile) {
            firstProfile = false
            return@LaunchedEffect
        }
        showTip = true
        delay(4500)
        showTip = false
    }

    // Shutter flash: a white veil that fades out after each photo.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(captureFlashTick) {
        if (captureFlashTick > 0) {
            flash.snapTo(0.75f)
            flash.animateTo(0f, tween(260))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // --- viewfinder gestures ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Only a press held past the tap window peeks at human vision, so
                            // a quick tap-to-focus doesn't flash the normal view.
                            val released = withTimeoutOrNull(HOLD_DELAY_MS) { tryAwaitRelease() }
                            if (released == null) {
                                onHoldStart()
                                try {
                                    awaitRelease()
                                } finally {
                                    onHoldEnd()
                                }
                            }
                        },
                        onTap = { offset ->
                            focusPoint = offset
                            focusTick++
                            onTapFocus(offset.x / size.width, offset.y / size.height)
                        },
                    )
                },
        )
        focusPoint?.let { p ->
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 34.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
            }
        }

        if (displayMode == DisplayMode.SPLIT) SplitOverlay(profile, splitPosition, onSplitChange)

        // Light scrims only where chrome sits, so the scene stays visible.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(if (isLandscape) 96.dp else 150.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isLandscape) 150.dp else 300.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))),
        )

        TopChrome(
            profile = profile,
            onOpenInfo = onOpenInfo,
            isRecording = isRecording,
            recordingStartedAtMs = recordingStartedAtMs,
            showStats = showStats,
            fps = fps,
            hasFlash = hasFlash,
            torchOn = torchOn,
            onToggleTorch = onToggleTorch,
            onSwitchCamera = onSwitchCamera,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(end = if (isLandscape) LandscapeControlsWidth - 12.dp else 0.dp),
        )

        // --- bottom deck ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(end = if (isLandscape) LandscapeControlsWidth else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TransientMessage(
                text = when {
                    statusMessage != null -> statusMessage
                    showModeHint -> modeLabel(displayMode, profile)
                    showTip -> tryTip(profile.id)
                    else -> null
                },
            )
            if (educationalMode && !isLandscape) {
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenInfo)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoomSelector(zoom = zoom, maxZoom = maxZoom, onZoom = onZoom)
                ModeSelector(displayMode = displayMode, onSetMode = onSetMode)
            }
            Spacer(Modifier.height(4.dp))
            AnimalSelector(profiles = profiles, selectedId = profile.id, onSelect = onSelectProfile)
            if (!isLandscape) {
                Spacer(Modifier.height(6.dp))
                ShutterRow(
                    lastCapture = lastCapture,
                    onOpenLastCapture = onOpenLastCapture,
                    onCapturePhoto = onCapturePhoto,
                    isRecording = isRecording,
                    onToggleRecord = onToggleRecord,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (isLandscape) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .width(LandscapeControlsWidth)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                ShutterRow(
                    lastCapture = lastCapture,
                    onOpenLastCapture = onOpenLastCapture,
                    onCapturePhoto = onCapturePhoto,
                    isRecording = isRecording,
                    onToggleRecord = onToggleRecord,
                    vertical = true,
                )
            }
        }

        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }
    }
}

/** Animal pill, honesty badges, REC timer and the camera/settings buttons. */
@Composable
private fun TopChrome(
    profile: AnimalVisionProfile,
    onOpenInfo: () -> Unit,
    isRecording: Boolean,
    recordingStartedAtMs: Long,
    showStats: Boolean,
    fps: Double,
    hasFlash: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimalPill(profile, onOpenInfo)
            Spacer(Modifier.weight(1f))
            if (isRecording) RecTimer(recordingStartedAtMs)
            Spacer(Modifier.weight(1f))
            if (hasFlash) {
                GlassIconButton(
                    icon = if (torchOn) AppIcons.FlashOn else AppIcons.FlashOff,
                    contentDesc = if (torchOn) "Turn flash off" else "Turn flash on",
                    active = torchOn,
                    onClick = onToggleTorch,
                )
            }
            GlassIconButton(AppIcons.CameraSwitch, "Switch between rear and front camera", false, onSwitchCamera)
            GlassIconButton(Icons.Filled.Settings, "Open settings", false, onOpenSettings)
        }
        val notices = remember(profile.id) { simulationNotices(profile) }
        if (notices.isNotEmpty() || showStats) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                notices.forEach { notice ->
                    Badge(
                        text = notice.badge,
                        color = AvWarning,
                        description = notice.detail,
                        onClick = onOpenInfo,
                    )
                }
                if (showStats && fps > 0) Badge(text = "${fps.toInt()} fps", color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun AnimalPill(profile: AnimalVisionProfile, onOpenInfo: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onOpenInfo)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Showing ${profile.name} vision. Open science and confidence info."
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CartoonAnimalFace(profile.id, Modifier.size(32.dp).clip(CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = profile.shortName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AvTextPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.Info, contentDescription = null, tint = AvTextPrimary.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RecTimer(startedAtMs: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val secs = ((now - startedAtMs).coerceAtLeast(0L) / 1000L).toInt()
    val label = "%02d:%02d".format(secs / 60, secs % 60)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Recording, $label" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(AvDanger))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun Badge(text: String, color: Color, description: String? = null, onClick: (() -> Unit)? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    )
}

/** Circular translucent control; active state tints it with the accent. */
@Composable
private fun GlassIconButton(icon: ImageVector, contentDesc: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else AvTextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Status toasts and the mode hint share one slot above the deck. */
@Composable
private fun TransientMessage(text: String?) {
    var shown by remember { mutableStateOf("") }
    if (text != null) shown = text
    AnimatedVisibility(visible = text != null, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = shown,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * Split mode: names both halves and shows a draggable divider. Wiping the boundary back and
 * forth over the same detail makes subtle (but real) differences easy to see.
 */
@Composable
private fun SplitOverlay(profile: AnimalVisionProfile, split: Float, onSplitChange: (Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val dividerX = with(density) { (widthPx * split).toDp() }
        // The drag handler outlives recompositions; read the latest position, not the
        // value captured when the gesture detector started.
        val currentSplit by rememberUpdatedState(split)
        // Thin line, fading out before the bottom controls.
        Box(
            Modifier
                .offset(x = dividerX - 0.75.dp)
                .width(1.5.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.7f),
                        0.62f to Color.White.copy(alpha = 0.7f),
                        0.7f to Color.Transparent,
                    ),
                ),
        )
        // Drag target: a 56dp strip along the line with a visible handle.
        Box(
            Modifier
                .offset(x = dividerX - 28.dp)
                .width(56.dp)
                .fillMaxHeight(0.62f)
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        onSplitChange((currentSplit + dragAmount / widthPx).coerceIn(0.1f, 0.9f))
                    }
                }
                .semantics { contentDescription = "Split divider, drag to compare" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹ ›",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvInk,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .wrapContentHeight(Alignment.CenterVertically),
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Below the status bar + top chrome (buttons and badge row), in both orientations.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = SplitLabelTop),
        ) {
            Box(Modifier.weight(split.coerceIn(0.05f, 0.95f)), contentAlignment = Alignment.Center) {
                Badge(text = profile.shortName.uppercase(), color = MaterialTheme.colorScheme.primary)
            }
            Box(Modifier.weight((1f - split).coerceIn(0.05f, 0.95f)), contentAlignment = Alignment.Center) {
                Badge(text = "HUMAN", color = Color.White)
            }
        }
    }
}

/** Zoom presets the camera supports; zooming also rescales the acuity model to the field shown. */
@Composable
private fun ZoomSelector(zoom: Float, maxZoom: Float, onZoom: (Float) -> Unit) {
    val presets = listOf(1f, 2f, 4f).filter { it <= maxZoom + 0.01f }
    if (presets.size < 2) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        presets.forEach { preset ->
            val selected = Math.abs(zoom - preset) < 0.05f
            Text(
                text = "${preset.toInt()}×",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AvInk else Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) AvAccentAlt else Color.Transparent)
                    .clickable { onZoom(preset) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                        contentDescription = "Zoom ${preset.toInt()} times"
                    },
            )
        }
    }
}

private fun modeLabel(mode: Int, profile: AnimalVisionProfile): String = when (mode) {
    DisplayMode.NORMAL -> "Human view — unprocessed camera"
    DisplayMode.SPLIT -> "Split — ${profile.shortName} left, human right"
    else -> "${profile.shortName} view"
}
