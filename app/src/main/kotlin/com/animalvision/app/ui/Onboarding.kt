package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    /** Cartoon ids shown as the page illustration. */
    val animals: List<String>,
    /** The page that offers the camera permission request. */
    val asksCamera: Boolean = false,
)

private val pages = listOf(
    OnboardingPage(
        "See through other eyes",
        "Fourteen animals, from dogs and eagles to bees and mantis shrimp. Point the camera " +
            "at the world and pick one: the live view changes to that species' color, detail, " +
            "field of view, and low-light vision.",
        listOf("dog", "bee", "eagle", "octopus"),
    ),
    OnboardingPage(
        "Pick, compare, capture",
        "Swipe the animal strip to switch species. Animal, Split, and Human change what you " +
            "compare; press and hold the view to peek at human vision, tap to focus. " +
            "Photos and videos save to your gallery.",
        listOf("cat", "human"),
    ),
    OnboardingPage(
        "Science, not gimmicks",
        "Every value comes from published measurements, with confidence ratings in each " +
            "animal's info. What a phone cannot sense is always labeled: ultraviolet, thermal, " +
            "polarization, and vision too fast for any screen.",
        listOf("mantisshrimp", "snake", "butterfly"),
    ),
    OnboardingPage(
        "Your camera stays private",
        "Animal Vision needs the camera to work. Frames are processed only on this device — " +
            "nothing is uploaded, and the microphone is never used.",
        listOf("pigeon", "horse"),
        asksCamera = true,
    ),
)

/**
 * First-run onboarding (spec §23): illustrated pages in a pager; the last one offers the
 * camera permission request. Skippable from any page.
 */
@Composable
fun OnboardingScreen(
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { pages.size }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize(), color = AvInk) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Status bar, navigation bar (bottom in portrait, side in landscape) and
                // cutouts — "Next" and "Skip" used to sit under the navigation bar.
                .safeDrawingPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDone) {
                    Text("Skip", color = AvMuted)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp)
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                        page.animals.forEach { id ->
                            CartoonAnimalFace(
                                id = id,
                                modifier = Modifier
                                    .size(if (isLandscape) 56.dp else 84.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, AvInk, CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.height(if (isLandscape) 12.dp else 28.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AvTextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AvMuted,
                        textAlign = TextAlign.Center,
                    )
                    if (page.asksCamera) {
                        Spacer(Modifier.height(24.dp))
                        if (cameraPermissionGranted) {
                            Text(
                                text = "Camera permission granted",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Button(onClick = onRequestCameraPermission) {
                                Text("Allow camera access")
                            }
                        }
                    }
                }
            }

            // Page indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else AvPanelElevated
                            )
                            .semantics {
                                contentDescription = "Page ${i + 1} of ${pages.size}"
                            },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.lastIndex) "Start exploring" else "Next",
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

