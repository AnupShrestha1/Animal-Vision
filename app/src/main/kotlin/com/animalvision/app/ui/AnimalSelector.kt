package com.animalvision.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animalvision.core.model.AnimalVisionProfile

/** Fixed strip height so the layouts around it can reserve exactly this much. */
val AnimalSelectorHeight = 92.dp

/**
 * Horizontal animal carousel (spec §18): cartoon portrait + short name per species, the
 * selected one ringed in the accent color. Scrolls the current animal into view on open.
 * Screen readers hear the full name and the overall confidence.
 */
@Composable
fun AnimalSelector(
    profiles: List<AnimalVisionProfile>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keep the current animal in view: on first show, after rotation, and when it changes.
    LaunchedEffect(selectedId) {
        val index = profiles.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any { it.index == index } &&
            visible.first().index < index && visible.last().index > index
        if (!fullyVisible) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().height(AnimalSelectorHeight),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(profiles, key = { it.id }) { profile ->
            val selected = profile.id == selectedId
            val overall = profile.scientificData.confidence.overall
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(profile.id) }
                    .padding(vertical = 4.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                        contentDescription = "${profile.name}, overall confidence ${overall.stars} out of 5"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CartoonAnimalFace(
                    id = profile.id,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = profile.shortName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
