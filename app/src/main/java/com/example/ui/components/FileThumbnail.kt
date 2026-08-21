package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileThumbnail(
    mimeType: String,
    localFilePath: String?,
    modifier: Modifier = Modifier
) {
    if (localFilePath == null) {
        DefaultFileIcon(modifier)
        return
    }
    
    val file = File(localFilePath)
    if (!file.exists()) {
        DefaultFileIcon(modifier)
        return
    }

    if (mimeType.startsWith("image/")) {
        AsyncImage(
            model = file,
            contentDescription = "Image Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (mimeType == "application/pdf") {
        PdfThumbnail(file = file, modifier = modifier)
    } else {
        DefaultFileIcon(modifier)
    }
}

@Composable
fun PdfThumbnail(file: File, modifier: Modifier = Modifier) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        if (renderer.pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                // Render to a reasonably small bitmap for thumbnail
                                val thumbWidth = 256
                                val thumbHeight = ((thumbWidth.toFloat() / page.width) * page.height).toInt().coerceIn(1, 512)
                                val bmp = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                                
                                // Fill background white
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Graceful fallback to default icon
            }
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = "PDF Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        DefaultFileIcon(modifier)
    }
}

@Composable
fun DefaultFileIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = "File",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
