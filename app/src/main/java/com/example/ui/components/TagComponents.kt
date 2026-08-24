package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import java.util.Locale
import kotlin.math.abs

/**
 * Standard preset tags recommended for file and secret categorization.
 */
val DefaultPresetTags = listOf(
    "Work",
    "Personal",
    "Project",
    "Finance",
    "Confidential",
    "Archive"
)

data class TagColorStyle(
    val textColor: Color,
    val backgroundColor: Color,
    val borderColor: Color
)

object TagColorHelper {
    fun getTagColors(tag: String): TagColorStyle {
        val normalized = tag.trim().lowercase(Locale.getDefault())
        return when (normalized) {
            "work" -> TagColorStyle(
                textColor = CyberCyanBright,
                backgroundColor = CyberCyan.copy(alpha = 0.16f),
                borderColor = CyberCyan.copy(alpha = 0.5f)
            )
            "personal" -> TagColorStyle(
                textColor = CyberEmeraldBright,
                backgroundColor = CyberEmerald.copy(alpha = 0.16f),
                borderColor = CyberEmerald.copy(alpha = 0.5f)
            )
            "project" -> TagColorStyle(
                textColor = Color(0xFFFBBF24), // Amber 400
                backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.18f),
                borderColor = Color(0xFFF59E0B).copy(alpha = 0.55f)
            )
            "finance" -> TagColorStyle(
                textColor = Color(0xFFC084FC), // Purple 400
                backgroundColor = Color(0xFFA855F7).copy(alpha = 0.18f),
                borderColor = Color(0xFFA855F7).copy(alpha = 0.55f)
            )
            "confidential" -> TagColorStyle(
                textColor = Color(0xFFFB7185), // Rose 400
                backgroundColor = Color(0xFFE11D48).copy(alpha = 0.18f),
                borderColor = Color(0xFFE11D48).copy(alpha = 0.55f)
            )
            "archive" -> TagColorStyle(
                textColor = Color(0xFF94A3B8), // Slate 400
                backgroundColor = Color(0xFF475569).copy(alpha = 0.22f),
                borderColor = Color(0xFF64748B).copy(alpha = 0.5f)
            )
            else -> {
                // Deterministic cyber palette for custom user tags
                val customPalette = listOf(
                    TagColorStyle(Color(0xFF38BDF8), Color(0xFF0284C7).copy(alpha = 0.18f), Color(0xFF0284C7).copy(alpha = 0.5f)), // Sky
                    TagColorStyle(Color(0xFF34D399), Color(0xFF059669).copy(alpha = 0.18f), Color(0xFF059669).copy(alpha = 0.5f)), // Emerald
                    TagColorStyle(Color(0xFFF472B6), Color(0xFFDB2777).copy(alpha = 0.18f), Color(0xFFDB2777).copy(alpha = 0.5f)), // Pink
                    TagColorStyle(Color(0xFF2DD4BF), Color(0xFF0D9488).copy(alpha = 0.18f), Color(0xFF0D9488).copy(alpha = 0.5f)), // Teal
                    TagColorStyle(Color(0xFFA78BFA), Color(0xFF7C3AED).copy(alpha = 0.18f), Color(0xFF7C3AED).copy(alpha = 0.5f)), // Violet
                    TagColorStyle(Color(0xFFFB923C), Color(0xFFEA580C).copy(alpha = 0.18f), Color(0xFFEA580C).copy(alpha = 0.5f)), // Orange
                    TagColorStyle(Color(0xFF818CF8), Color(0xFF4F46E5).copy(alpha = 0.18f), Color(0xFF4F46E5).copy(alpha = 0.5f))  // Indigo
                )
                val index = abs(normalized.hashCode()) % customPalette.size
                customPalette[index]
            }
        }
    }
}

/**
 * Individual Tag Pill Badge
 */
@Composable
fun TagBadge(
    tag: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSmall: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = TagColorHelper.getTagColors(tag)
    val fontSize = if (isSmall) 9.sp else 11.sp
    val verticalPadding = if (isSmall) 2.dp else 4.dp
    val horizontalPadding = if (isSmall) 6.dp else 8.dp

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) colors.textColor.copy(alpha = 0.25f) else colors.backgroundColor,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) colors.textColor else colors.borderColor
        ),
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .testTag("tag_badge_${tag.lowercase(Locale.getDefault())}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#$tag",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = colors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(14.dp)
                        .testTag("remove_tag_${tag.lowercase(Locale.getDefault())}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove tag $tag",
                        tint = colors.textColor,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders a compact row of TagBadges on a list card or header.
 */
@Composable
fun TagBadgeList(
    tags: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    onTagClick: ((String) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    if (tags.isEmpty() && onEditClick == null) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val visibleTags = tags.take(maxVisible)
        val overflowCount = tags.size - maxVisible

        visibleTags.forEach { tag ->
            TagBadge(
                tag = tag,
                isSmall = true,
                onClick = if (onTagClick != null) { { onTagClick(tag) } } else null
            )
        }

        if (overflowCount > 0) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (onEditClick != null) Modifier.clickable { onEditClick() } else Modifier
                    )
            ) {
                Text(
                    text = "+$overflowCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        if (onEditClick != null && tags.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onEditClick() }
                    .testTag("add_tag_pill_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add tag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Tag",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Filter Bar for Tags on History or Dashboard Screens.
 */
@Composable
fun TagFilterRow(
    allTags: List<String>,
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    tagCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (allTags.isEmpty()) return

    LazyRow(
        modifier = modifier.testTag("tag_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        // "All Tags" Option
        item(key = "tag_filter_all") {
            val isAllSelected = selectedTag == null
            FilterChip(
                selected = isAllSelected,
                onClick = { onSelectTag(null) },
                label = {
                    Text(
                        text = "All Tags",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isAllSelected) CyberCyanBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                    selectedLabelColor = CyberCyanBright
                ),
                modifier = Modifier.testTag("tag_filter_all_chip")
            )
        }

        // Each distinct tag option
        items(allTags, key = { "tag_filter_$it" }) { tag ->
            val isSelected = selectedTag.equals(tag, ignoreCase = true)
            val colors = TagColorHelper.getTagColors(tag)
            val count = tagCounts[tag]

            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        onSelectTag(null) // Toggle off
                    } else {
                        onSelectTag(tag)
                    }
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        if (count != null && count > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) colors.textColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) colors.textColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.backgroundColor,
                    selectedLabelColor = colors.textColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) colors.textColor else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.testTag("tag_filter_${tag.lowercase(Locale.getDefault())}")
            )
        }
    }
}

/**
 * Inline Tag Selector for Send Screen to categorize outbound transmissions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendTagSelector(
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    onAddCustomTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customTagText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("send_tag_selector_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TAGS & CATEGORIZATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = CyberCyanBright
                    )
                }

                if (selectedTags.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${selectedTags.size} selected",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CyberCyanBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Categorize this payload as Work, Personal, Project, or custom tags for easy filtering:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Standard Preset Tag Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availablePresetTags = (DefaultPresetTags + selectedTags).distinct()
                availablePresetTags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag)
                    val colors = TagColorHelper.getTagColors(tag)

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isSelected) colors.backgroundColor else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.textColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { onToggleTag(tag) }
                            .testTag("send_tag_chip_${tag.lowercase(Locale.getDefault())}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.textColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSelected) colors.textColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Add Custom Tag Button
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { showCustomInput = !showCustomInput }
                        .testTag("send_add_custom_tag_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showCustomInput) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add custom tag",
                            tint = CyberEmeraldBright,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (showCustomInput) "Cancel" else "Add Tag",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberEmeraldBright
                        )
                    }
                }
            }

            // Custom Tag Input Expansion
            AnimatedVisibility(visible = showCustomInput) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTagText,
                            onValueChange = { customTagText = it.take(20).replace(" ", "_") },
                            placeholder = { Text("e.g. Sprint_12, Tax_2026", style = MaterialTheme.typography.labelMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_custom_tag_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyanBright,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val cleaned = customTagText.trim().removePrefix("#")
                                    if (cleaned.isNotEmpty()) {
                                        onAddCustomTag(cleaned)
                                        customTagText = ""
                                        showCustomInput = false
                                    }
                                }
                            )
                        )

                        Button(
                            onClick = {
                                val cleaned = customTagText.trim().removePrefix("#")
                                if (cleaned.isNotEmpty()) {
                                    onAddCustomTag(cleaned)
                                    customTagText = ""
                                    showCustomInput = false
                                }
                            },
                            enabled = customTagText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            modifier = Modifier.testTag("send_confirm_custom_tag_btn")
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Tag Editor Dialog for managing tags on any sent or received record.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    initialTags: List<String>,
    allExistingTags: List<String> = emptyList(),
    fileName: String = "Transfer Record",
    onSaveTags: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTags by remember { mutableStateOf(initialTags.toSet()) }
    var newTagInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val combinedSuggestions = remember(allExistingTags, currentTags) {
        (DefaultPresetTags + allExistingTags).distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("tag_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = CyberCyanBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Categorize & Tag",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tag_editor_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Active Attached Tags
                Text(
                    text = "ACTIVE ATTACHED TAGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = CyberCyanBright
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (currentTags.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tags assigned yet. Tap preset tags below or type a custom tag.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTags.forEach { tag ->
                            TagBadge(
                                tag = tag,
                                onRemove = { currentTags = currentTags - tag }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Create Custom Tag
                Text(
                    text = "ADD CUSTOM TAG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = CyberEmeraldBright
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it.take(20).replace(" ", "_") },
                        placeholder = { Text("Tag name (e.g. Work, Project)...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null, tint = CyberEmeraldBright, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_editor_custom_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmeraldBright,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val cleaned = newTagInput.trim().removePrefix("#")
                                if (cleaned.isNotEmpty()) {
                                    currentTags = currentTags + cleaned
                                    newTagInput = ""
                                }
                            }
                        )
                    )

                    Button(
                        onClick = {
                            val cleaned = newTagInput.trim().removePrefix("#")
                            if (cleaned.isNotEmpty()) {
                                currentTags = currentTags + cleaned
                                newTagInput = ""
                            }
                        },
                        enabled = newTagInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier.testTag("tag_editor_add_btn")
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Suggested & Preset Tags
                Text(
                    text = "PRESET & SUGGESTED CATEGORIES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    combinedSuggestions.forEach { tag ->
                        val isAssigned = currentTags.contains(tag)
                        val colors = TagColorHelper.getTagColors(tag)

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isAssigned) colors.backgroundColor else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isAssigned) 1.5.dp else 1.dp,
                                color = if (isAssigned) colors.textColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable {
                                    currentTags = if (isAssigned) {
                                        currentTags - tag
                                    } else {
                                        currentTags + tag
                                    }
                                }
                                .testTag("tag_suggestion_${tag.lowercase(Locale.getDefault())}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isAssigned) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = colors.textColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isAssigned) colors.textColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Save / Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_editor_cancel_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSaveTags(currentTags.toList())
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_editor_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Tags")
                    }
                }
            }
        }
    }
}
