package com.animalvision.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animalvision.core.model.AnimalVisionProfile

/**
 * Horizontal animal selector strip (spec §18): each chip announces name + overall
 * confidence as a screen-reader label.
 */
@Composable
fun AnimalSelector(
    profiles: List<AnimalVisionProfile>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(profiles, key = { it.id }) { profile ->
            val selected = profile.id == selectedId
            val overall = profile.scientificData.confidence.overall
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else AvPanel.copy(alpha = 0.82f)
                    )
                    .then(
                        if (selected) {
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(profile.id) }
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .semantics {
                        role = Role.Tab
                        contentDescription =
                            "${profile.name}, overall confidence ${overall.stars} out of 5"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = animalGlyph(profile.id),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else AvTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "★".repeat(overall.stars),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) AvAccentAlt else AvMuted,
                )
            }
        }
    }
}

/** Simple text glyphs — avoids shipping icon assets while staying readable (spec §36). */
fun animalGlyph(id: String): String = when (id) {
    "human" -> "◑"
    "dog" -> "◉"
    "cat" -> "◐"
    "horse" -> "◈"
    "eagle" -> "✦"
    "bee" -> "✤"
    "snake" -> "∿"
    else -> "○"
}
