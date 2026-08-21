package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferMode
import com.example.data.TransferRecord
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.util.DateFormatter
import com.example.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterOption(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month")
}

@Composable
fun HistoryScreen(
    transfers: List<TransferRecord>,
    onInspectTransfer: (TransferRecord) -> Unit,
    onToggleFavorite: (TransferRecord) -> Unit,
    onDeleteTransfer: (TransferRecord) -> Unit,
    onDeleteTransfers: (List<TransferRecord>) -> Unit = { list -> list.forEach { onDeleteTransfer(it) } },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterOption.ALL) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedRecordIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportSelectionOnly by remember { mutableStateOf(false) }

    val filteredTransfers = remember(transfers, searchQuery, selectedFilter, selectedDateFilter) {
        val trimmedQuery = searchQuery.trim().lowercase(Locale.getDefault())

        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - DateUtils.DAY_IN_MILLIS
        val sevenDaysAgo = todayStart - (6 * DateUtils.DAY_IN_MILLIS)
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        transfers.filter { record ->
            // Date Filter Check
            val matchesDateFilter = when (selectedDateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> record.timestamp >= todayStart
                DateFilterOption.YESTERDAY -> record.timestamp in yesterdayStart until todayStart
                DateFilterOption.LAST_7_DAYS -> record.timestamp >= sevenDaysAgo
                DateFilterOption.THIS_MONTH -> record.timestamp >= monthStart
            }

            // Category & Status Filter Check
            val matchesCategoryFilter = when (selectedFilter) {
                "Success" -> record.status.isSuccess
                "Failed" -> record.status.isFailed
                "Pending" -> record.status.isPending
                "Sent" -> !record.isReceived
                "Received" -> record.isReceived
                "Favorites" -> record.isFavorite
                "QR Stream" -> record.transferMode == TransferMode.QR_STREAM
                "P2P LAN" -> record.transferMode == TransferMode.P2P_DIRECT
                "Secrets" -> record.transferMode == TransferMode.QR_SECRET || record.decryptedTextPreview != null
                else -> true
            }

            // Search Query Check (Matches File Name, Team Name, and All Date Formats)
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                val recordDate = Date(record.timestamp)
                val matchesFileName = record.fileName.lowercase(Locale.getDefault()).contains(trimmedQuery)
                val matchesTeamName = record.teamName.lowercase(Locale.getDefault()).contains(trimmedQuery)
                val matchesTextPreview = record.decryptedTextPreview?.lowercase(Locale.getDefault())?.contains(trimmedQuery) == true
                
                // Formatted date string comparisons for robust date search
                val dateFormats = listOf(
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
                    SimpleDateFormat("MMM d", Locale.getDefault()),
                    SimpleDateFormat("MMMM d", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("yyyy", Locale.getDefault()),
                    SimpleDateFormat("MMMM", Locale.getDefault()),
                    SimpleDateFormat("MMM", Locale.getDefault()),
                    SimpleDateFormat("EEEE", Locale.getDefault()),
                    SimpleDateFormat("HH:mm", Locale.getDefault()),
                    SimpleDateFormat("h:mm a", Locale.getDefault())
                )

                val matchesDateString = dateFormats.any { format ->
                    format.format(recordDate).lowercase(Locale.getDefault()).contains(trimmedQuery)
                }

                val matchesRelativeDate = when {
                    trimmedQuery == "today" -> DateUtils.isToday(record.timestamp)
                    trimmedQuery == "yesterday" -> record.timestamp in yesterdayStart until todayStart
                    else -> false
                }

                matchesFileName || matchesTeamName || matchesTextPreview || matchesDateString || matchesRelativeDate
            }

            matchesDateFilter && matchesCategoryFilter && matchesQuery
        }
    }

    val isAnyFilterActive = searchQuery.isNotEmpty() || selectedFilter != "All" || selectedDateFilter != DateFilterOption.ALL

    val totalSuccessfulTransfers = remember(transfers) {
        transfers.count { it.status.isSuccess }
    }
    val totalDataVolumeBytes = remember(transfers) {
        transfers.sumOf { if (it.originalSize > 0) it.originalSize else it.encryptedSize }
    }
    val filteredSuccessfulTransfers = remember(filteredTransfers) {
        filteredTransfers.count { it.status.isSuccess }
    }
    val filteredDataVolumeBytes = remember(filteredTransfers) {
        filteredTransfers.sumOf { if (it.originalSize > 0) it.originalSize else it.encryptedSize }
    }

    val selectedRecords = remember(transfers, selectedRecordIds) {
        transfers.filter { selectedRecordIds.contains(it.id) }
    }
    val totalSelectedBytes = remember(selectedRecords) {
        selectedRecords.sumOf { it.originalSize }
    }

    // Bulk Delete Confirmation Dialog
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedRecordIds.size} Transfer${if (selectedRecordIds.size > 1) "s" else ""}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete the selected ${selectedRecordIds.size} record(s) (${FileUtils.formatBytes(totalSelectedBytes)}) from local storage?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Decrypted files stored on device will be permanently purged. This action cannot be undone.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedRecords
                        onDeleteTransfers(toDelete)
                        selectedRecordIds = emptySet()
                        isSelectionMode = false
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("history_bulk_delete_confirm_button")
                ) {
                    Text("Delete (${selectedRecordIds.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkDeleteDialog = false },
                    modifier = Modifier.testTag("history_bulk_delete_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("history_bulk_delete_dialog")
        )
    }

    // Export History Logs Dialog (CSV / JSON with System Share Intent)
    if (showExportDialog) {
        val targetRecords = if (exportSelectionOnly) {
            transfers.filter { selectedRecordIds.contains(it.id) }
        } else {
            if (filteredTransfers.isNotEmpty()) filteredTransfers else transfers
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = CyberCyanBright,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Export Transfer Audit Logs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Export ${targetRecords.size} transfer audit records with SHA-256 integrity checksums, safety numbers, timestamps, and metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: CSV Spreadsheet
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showExportDialog = false
                                FileUtils.exportHistoryToCsv(context, targetRecords)
                            }
                            .testTag("export_csv_option_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberEmerald.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = CyberEmeraldBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CSV Spreadsheet (.csv)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Standard table format for Excel, Google Sheets, or Numbers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Share",
                                tint = CyberEmeraldBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Option 2: JSON Archive
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showExportDialog = false
                                FileUtils.exportHistoryToJson(context, targetRecords)
                            }
                            .testTag("export_json_option_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = CyberCyanBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "JSON Archive (.json)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Structured developer format for log analysis and backup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Share",
                                tint = CyberCyanBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    modifier = Modifier.testTag("history_export_cancel_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("history_export_dialog")
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Contextual Header: Selection Mode vs Normal Mode
            if (isSelectionMode) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_selection_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedRecordIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("history_exit_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "${selectedRecordIds.size} Selected",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = CyberCyanBright
                                )
                                if (selectedRecordIds.isNotEmpty()) {
                                    Text(
                                        text = FileUtils.formatBytes(totalSelectedBytes),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Select All / Deselect All Button
                            val allFilteredSelected = filteredTransfers.isNotEmpty() &&
                                    filteredTransfers.all { selectedRecordIds.contains(it.id) }

                            OutlinedButton(
                                onClick = {
                                    selectedRecordIds = if (allFilteredSelected) {
                                        emptySet()
                                    } else {
                                        filteredTransfers.map { it.id }.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberCyanBright
                                ),
                                modifier = Modifier.testTag("history_select_all_button")
                            ) {
                                Icon(
                                    imageVector = if (allFilteredSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allFilteredSelected) "None" else "All",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Export Action Button (Selection)
                            OutlinedButton(
                                onClick = {
                                    exportSelectionOnly = true
                                    showExportDialog = true
                                },
                                enabled = selectedRecordIds.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = if (selectedRecordIds.isNotEmpty()) 0.6f else 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberCyanBright,
                                    disabledContentColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("history_bulk_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Selection",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Export",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Delete Action Button
                            Button(
                                onClick = { showBulkDeleteDialog = true },
                                enabled = selectedRecordIds.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF334155),
                                    disabledContentColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.testTag("history_bulk_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Bulk Delete",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            } else {
                // Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transfer History",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAnyFilterActive) {
                                "Showing ${filteredTransfers.size} of ${transfers.size} records"
                            } else {
                                "${transfers.size} decrypted files & payloads on device"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAnyFilterActive) CyberCyanBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAnyFilterActive) {
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = "All"
                                    selectedDateFilter = DateFilterOption.ALL
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberCyanBright
                                ),
                                modifier = Modifier.testTag("history_reset_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Filters",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (transfers.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    exportSelectionOnly = false
                                    showExportDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberCyanBright
                                ),
                                modifier = Modifier.testTag("history_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export History",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    isSelectionMode = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberEmeraldBright
                                ),
                                modifier = Modifier.testTag("history_enter_selection_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select items",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced Search Bar (Filters by File Name or Date)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search by file name or date (e.g. Aug 16, 2026, today)...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = if (searchQuery.isNotEmpty()) CyberCyanBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("history_search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = CyberCyanBright
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyanBright,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Date Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(DateFilterOption.entries.toTypedArray()) { option ->
                    val isSelected = selectedDateFilter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDateFilter = option },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = CyberCyanBright
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyanBright
                        ),
                        modifier = Modifier.testTag("history_date_filter_${option.name.lowercase(Locale.getDefault())}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category & Status Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                val filterOptions = listOf(
                    "All",
                    "Success",
                    "Failed",
                    "Pending",
                    "Favorites",
                    "Sent",
                    "Received",
                    "QR Stream",
                    "P2P LAN",
                    "Secrets"
                )
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipLeadingIcon: (@Composable () -> Unit)? = when (filter) {
                        "Success" -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isSelected) CyberEmeraldBright else CyberEmerald
                                )
                            }
                        }
                        "Failed" -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isSelected) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.7f)
                                )
                            }
                        }
                        "Pending" -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isSelected) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.7f)
                                )
                            }
                        }
                        else -> null
                    }

                    val containerColor = when (filter) {
                        "Success" -> CyberEmerald.copy(alpha = 0.2f)
                        "Failed" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        "Pending" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        else -> CyberEmerald.copy(alpha = 0.2f)
                    }

                    val labelColor = when (filter) {
                        "Success" -> CyberEmeraldBright
                        "Failed" -> Color(0xFFEF4444)
                        "Pending" -> Color(0xFFF59E0B)
                        else -> CyberEmeraldBright
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = chipLeadingIcon,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = containerColor,
                            selectedLabelColor = labelColor
                        ),
                        modifier = Modifier.testTag("history_filter_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // History List
            if (filteredTransfers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (transfers.isNotEmpty()) {
                        HistorySummaryCard(
                            totalSuccessful = totalSuccessfulTransfers,
                            totalTransfers = transfers.size,
                            totalVolumeBytes = totalDataVolumeBytes,
                            filteredCount = filteredTransfers.size,
                            isFiltered = isAnyFilterActive
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isAnyFilterActive) Icons.Default.Search else Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isAnyFilterActive) "No matching records found" else "History is empty",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAnyFilterActive) {
                                    "Try searching with a different file name, date format, or clearing filters."
                                } else {
                                    "Received or sent encrypted files will be archived here securely."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (isAnyFilterActive) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilter = "All"
                                        selectedDateFilter = DateFilterOption.ALL
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                                ) {
                                    Text("Clear Search & Filters", color = CyberCyanBright)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = if (isSelectionMode && selectedRecordIds.isNotEmpty()) 140.dp else 96.dp)
                ) {
                    // Summary Card at top of History List
                    item(key = "history_summary_card_header") {
                        HistorySummaryCard(
                            totalSuccessful = totalSuccessfulTransfers,
                            totalTransfers = transfers.size,
                            totalVolumeBytes = totalDataVolumeBytes,
                            filteredCount = filteredTransfers.size,
                            isFiltered = isAnyFilterActive
                        )
                    }

                    items(filteredTransfers, key = { it.id }) { record ->
                        val isSelected = selectedRecordIds.contains(record.id)
                        SwipeableTransferListItem(
                            record = record,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedRecordIds = if (isSelected) {
                                        selectedRecordIds - record.id
                                    } else {
                                        selectedRecordIds + record.id
                                    }
                                } else {
                                    onInspectTransfer(record)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedRecordIds = setOf(record.id)
                                } else {
                                    selectedRecordIds = if (isSelected) {
                                        selectedRecordIds - record.id
                                    } else {
                                        selectedRecordIds + record.id
                                    }
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(record) },
                            onShare = {
                                FileUtils.shareText(
                                    context,
                                    record.decryptedTextPreview ?: record.fileName,
                                    record.fileName
                                )
                            },
                            onDelete = { onDeleteTransfer(record) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Multi-Delete Bar
        AnimatedVisibility(
            visible = isSelectionMode && selectedRecordIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.8f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_floating_bulk_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedRecordIds.size} file${if (selectedRecordIds.size > 1) "s" else ""} selected",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = FileUtils.formatBytes(totalSelectedBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = CyberCyanBright
                        )
                    }

                    Button(
                        onClick = { showBulkDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("history_floating_bulk_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete (${selectedRecordIds.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransferListItem(
    record: TransferRecord,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            val isSwipingToDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val backgroundColor = if (isSwipingToDelete) Color(0xFFDC2626) else Color(0xFFEF4444).copy(alpha = 0.85f)
            val scale by animateFloatAsState(
                targetValue = if (isSwipingToDelete) 1.2f else 0.9f,
                label = "swipe_icon_scale_${record.id}"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp)
                    .testTag("swipe_to_delete_bg_${record.id}"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to delete",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        TransferListItem(
            record = record,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare
        )
    }
}

@Composable
fun HistorySummaryCard(
    totalSuccessful: Int,
    totalTransfers: Int,
    totalVolumeBytes: Long,
    filteredCount: Int,
    isFiltered: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.25f))
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TRANSFER AUDIT OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = CyberCyanBright
                    )
                }

                if (isFiltered) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Showing $filteredCount of $totalTransfers",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = CyberCyanBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$totalTransfers Total Archived",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = CyberEmeraldBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Total Successful Transfers
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_summary_success_card"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Successful Transfers",
                                tint = CyberEmeraldBright,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$totalSuccessful",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = CyberEmeraldBright,
                                modifier = Modifier.testTag("history_summary_success_count")
                            )
                            Text(
                                text = "Successful Transfers",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Metric 2: Total Data Volume Processed
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("history_summary_volume_card"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "Data Volume Processed",
                                tint = CyberCyanBright,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = FileUtils.formatBytes(totalVolumeBytes),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = CyberCyanBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("history_summary_data_volume")
                            )
                            Text(
                                text = "Volume Processed",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

