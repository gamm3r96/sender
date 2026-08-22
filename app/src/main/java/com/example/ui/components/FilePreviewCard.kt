package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldBright
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.CyberVioletBright
import com.example.util.FileUtils
import java.io.File

enum class DetectedFileType(
    val categoryName: String,
    val icon: ImageVector,
    val primaryColor: Color
) {
    IMAGE("Image Photo", Icons.Default.Image, CyberEmeraldBright),
    TEXT_PLAIN("Plain Text", Icons.Default.Description, CyberCyanBright),
    CODE("Source Code", Icons.Default.Code, CyberCyanBright),
    JSON_DATA("Structured Data", Icons.Default.DataObject, CyberVioletBright),
    DOCUMENT("Document", Icons.Default.PictureAsPdf, CyberAmber),
    AUDIO("Audio Recording", Icons.Default.AudioFile, CyberEmerald),
    VIDEO("Video Media", Icons.Default.Movie, Color(0xFFE879F9)),
    ARCHIVE_BINARY("Binary Archive", Icons.Default.FolderZip, Color(0xFF94A3B8))
}

object FileTypeDetector {
    fun detect(fileName: String, mimeType: String): DetectedFileType {
        val lowerName = fileName.lowercase()
        val lowerMime = mimeType.lowercase()

        return when {
            lowerMime.startsWith("image/") || lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".bmp") || lowerName.endsWith(".svg") -> DetectedFileType.IMAGE

            lowerName.endsWith(".json") || lowerMime.contains("json") -> DetectedFileType.JSON_DATA

            lowerName.endsWith(".kt") || lowerName.endsWith(".java") || lowerName.endsWith(".py") ||
                    lowerName.endsWith(".js") || lowerName.endsWith(".ts") || lowerName.endsWith(".html") ||
                    lowerName.endsWith(".css") || lowerName.endsWith(".sql") || lowerName.endsWith(".xml") ||
                    lowerName.endsWith(".yaml") || lowerName.endsWith(".yml") || lowerName.endsWith(".sh") ||
                    lowerName.endsWith(".env") || lowerName.endsWith(".md") || lowerName.endsWith(".csv") -> DetectedFileType.CODE

            lowerMime.startsWith("text/") || lowerName.endsWith(".txt") || lowerName.endsWith(".log") ||
                    lowerName.endsWith(".conf") || lowerName.endsWith(".ini") -> DetectedFileType.TEXT_PLAIN

            lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                    lowerName.endsWith(".m4a") || lowerName.endsWith(".ogg") || lowerName.endsWith(".flac") -> DetectedFileType.AUDIO
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                    lowerName.endsWith(".webm") || lowerName.endsWith(".avi") || lowerName.endsWith(".mov") -> DetectedFileType.VIDEO

            lowerMime.contains("pdf") || lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") ||
                    lowerName.endsWith(".docx") || lowerName.endsWith(".xlsx") -> DetectedFileType.DOCUMENT

            else -> DetectedFileType.ARCHIVE_BINARY
        }
    }
}

/**
 * High-tech File Preview Component
 * Automatically detects file type and renders interactive thumbnails, code snippets with line numbers,
 * image dimensions, or hex inspection blocks before confirming save.
 */
@Composable
fun FilePreviewCard(
    fileName: String,
    mimeType: String,
    fileSize: Long,
    filePath: String?,
    rawTextPreview: String?,
    modifier: Modifier = Modifier,
    onCopyContent: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val detectedType = remember(fileName, mimeType) { FileTypeDetector.detect(fileName, mimeType) }

    var isExpanded by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }

    // Load file bytes / text / image safely
    val fileObj = remember(filePath) { filePath?.let { File(it) }?.takeIf { it.exists() } }

    val decodedImage = remember(filePath, detectedType) {
        if (detectedType == DetectedFileType.IMAGE && fileObj != null) {
            try {
                BitmapFactory.decodeFile(fileObj.absolutePath)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    val fileTextContent = remember(filePath, rawTextPreview, detectedType) {
        when {
            rawTextPreview != null && rawTextPreview.isNotBlank() -> rawTextPreview
            fileObj != null && (detectedType == DetectedFileType.TEXT_PLAIN || detectedType == DetectedFileType.CODE || detectedType == DetectedFileType.JSON_DATA) -> {
                try {
                    fileObj.bufferedReader(Charsets.UTF_8).use { reader ->
                        val lines = mutableListOf<String>()
                        var count = 0
                        while (count < 250) {
                            val line = reader.readLine() ?: break
                            lines.add(line)
                            count++
                        }
                        lines.joinToString("\n")
                    }
                } catch (_: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    val hexSnippet = remember(filePath, detectedType, fileTextContent) {
        if (fileTextContent == null && decodedImage == null && fileObj != null) {
            try {
                val bytes = fileObj.inputStream().use { it.readNBytes(48) }
                formatHexDump(bytes)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("file_preview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, detectedType.primaryColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Detected Category Badge & Quick Actions
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
                        shape = RoundedCornerShape(6.dp),
                        color = detectedType.primaryColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, detectedType.primaryColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = detectedType.icon,
                                contentDescription = null,
                                tint = detectedType.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = detectedType.categoryName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = detectedType.primaryColor
                            )
                        }
                    }

                    Text(
                        text = FileUtils.formatBytes(fileSize),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Copy Action if text exists
                if (fileTextContent != null) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fileTextContent))
                            onCopyContent?.invoke(fileTextContent)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_preview_content_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Content",
                            tint = CyberCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Preview Body: Image vs Text vs Hex vs Binary Info
            when {
                detectedType == DetectedFileType.VIDEO && fileObj != null -> {
                    VideoPreviewSection(filePath = fileObj.absolutePath)
                }
                detectedType == DetectedFileType.AUDIO && fileObj != null -> {
                    AudioPreviewSection(filePath = fileObj.absolutePath, fileName = fileName)
                }
                decodedImage != null -> {
                    ImagePreviewSection(
                        bitmap = decodedImage,
                        isZoomed = isImageZoomed,
                        onToggleZoom = { isImageZoomed = !isImageZoomed }
                    )
                }

                fileTextContent != null -> {
                    TextCodePreviewSection(
                        content = fileTextContent,
                        detectedType = detectedType,
                        isExpanded = isExpanded,
                        onToggleExpand = { isExpanded = !isExpanded }
                    )
                }

                hexSnippet != null -> {
                    HexDumpPreviewSection(
                        hexDump = hexSnippet
                    )
                }

                else -> {
                    GenericFilePlaceholder(
                        fileName = fileName,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        detectedType = detectedType
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewSection(
    bitmap: Bitmap,
    isZoomed: Boolean,
    onToggleZoom: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isZoomed) 320.dp else 190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { onToggleZoom() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image Preview Thumbnail",
                contentScale = if (isZoomed) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )

            // Zoom indicator floating badge
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        contentDescription = "Toggle Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isZoomed) "Fit" else "Enlarge",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
        }

        // Image Metadata Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${bitmap.width} × ${bitmap.height} px",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = CyberEmeraldBright
            )

            Text(
                text = "Bitmap Color: ${bitmap.config?.name ?: "ARGB_8888"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextCodePreviewSection(
    content: String,
    detectedType: DetectedFileType,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val lines = remember(content) { content.lines() }
    val displayLines = remember(content, isExpanded) {
        if (isExpanded) lines else lines.take(6)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Monospace Syntax Box with Gutter
        Surface(
            color = Color(0xFF0D1117),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isExpanded) 340.dp else 160.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Line Number Gutter
                Column(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .border(
                            width = 0.dp,
                            color = Color.Transparent
                        )
                ) {
                    displayLines.indices.forEach { index ->
                        Text(
                            text = String.format("%02d", index + 1),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF6E7681)
                        )
                    }
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height((displayLines.size * 17).dp)
                        .background(Color(0xFF30363D))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Content Snippet
                Column {
                    displayLines.forEach { line ->
                        Text(
                            text = line.ifEmpty { " " },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = if (detectedType == DetectedFileType.JSON_DATA) CyberVioletBright else Color(0xFFE6EDF3),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        // Footer Toolbar: Line Counter & Expand Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${lines.size} lines • ${content.length} characters",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lines.size > 6) {
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Collapse Snippet" else "Show Full (${lines.size} lines)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyanBright
                        )
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = CyberCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HexDumpPreviewSection(
    hexDump: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "RAW HEXADECIMAL HEADER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            ),
            color = CyberCyanBright
        )

        Surface(
            color = Color(0xFF090D16),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = hexDump,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                ),
                color = CyberEmeraldBright.copy(alpha = 0.9f),
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun GenericFilePlaceholder(
    fileName: String,
    mimeType: String,
    fileSize: Long,
    detectedType: DetectedFileType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = detectedType.primaryColor.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = detectedType.icon,
                    contentDescription = null,
                    tint = detectedType.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$mimeType • ${FileUtils.formatBytes(fileSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatHexDump(bytes: ByteArray): String {
    return buildString {
        for (i in bytes.indices step 16) {
            val chunk = bytes.sliceArray(i until minOf(i + 16, bytes.size))
            val hex = chunk.joinToString(" ") { String.format("%02X", it) }
            val ascii = chunk.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
            append(String.format("%04X  %-48s  |%s|\n", i, hex, ascii))
        }
    }.trimEnd()
}

@Composable
private fun VideoPreviewSection(filePath: String) {
    var isPlaying by remember { androidx.compose.runtime.mutableStateOf(true) }
    var videoView by remember { androidx.compose.runtime.mutableStateOf<android.widget.VideoView?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, Color(0xFFE879F9).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable {
                    videoView?.let {
                        if (it.isPlaying) {
                            it.pause()
                            isPlaying = false
                        } else {
                            it.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.widget.VideoView(context).apply {
                        setVideoPath(filePath)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                        videoView = this
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPreviewSection(filePath: String, fileName: String) {
    val context = LocalContext.current
    var isPlaying by remember { androidx.compose.runtime.mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { androidx.compose.runtime.mutableStateOf(0) }
    
    val mediaPlayer = remember(filePath) { 
        android.media.MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                duration = this.duration
            } catch (e: Exception) {}
        }
    }
    
    DisposableEffect(mediaPlayer) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    progress = mediaPlayer.currentPosition.toFloat() / duration.coerceAtLeast(1)
                } else {
                    isPlaying = false
                }
            } catch (e: Exception) {}
            kotlinx.coroutines.delay(250)
        }
    }

    Surface(
        color = Color(0xFF090D16),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AudioFile,
                contentDescription = null,
                tint = CyberEmeraldBright,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        try {
                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = CyberEmeraldBright
                    )
                }
                
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = CyberEmeraldBright,
                    trackColor = Color(0xFF1E293B)
                )
            }
        }
    }
}
