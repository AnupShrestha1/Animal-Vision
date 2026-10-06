package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        "How to use",
        "Point the camera at the world and pick an animal. The live view changes to that " +
            "species' vision model — color, detail, field of view, and low light. " +
            "Press and hold the viewfinder to peek at normal human vision at any time.",
    ),
    OnboardingPage(
        "Before / after",
        "Switch between Normal, Animal, and Split modes. Split shows animal vision on the " +
            "left and human vision on the right of a center divider. Hold-to-compare always " +
            "shows the unprocessed view while pressed.",
    ),
    OnboardingPage(
        "Science, not gimmicks",
        "Every parameter comes from published measurements, graded with confidence bars in " +
            "the info panel. Bee ultraviolet and snake thermal are labeled simulations — " +
            "phones cannot sense true UV or infrared, and we never pretend otherwise.",
    ),
    OnboardingPage(
        "Camera permission",
        "Animal Vision needs the camera to show you the world through another animal's eyes. " +
            "Frames are processed entirely on this device — nothing is uploaded, and no " +
            "storage permission is required to save captures.",
    ),
    OnboardingPage(
        "Capture",
        "Take photos and record videos of the processed view; they save to your gallery " +
            "with the animal name and timestamp. Enjoy exploring the visual worlds of " +
            "other species!",
    ),
)

/**
 * First-run onboarding (spec §23): 5 pages in a pager; page 4 offers the camera
 * permission request. Skippable from any page.
 */
@Composable
fun OnboardingScreen(
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize(), color = AvInk) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
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
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                    if (index == 3) {
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
                    modifier = Modifier.fillMaxWidth(),
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

