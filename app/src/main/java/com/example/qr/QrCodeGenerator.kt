package com.example.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import com.example.data.QrColorScheme
import com.example.data.QrErrorCorrectionLevel
import com.example.data.QrModuleShape
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    private val bitMatrixCache = LruCache<String, BitMatrix>(300)

    /**
     * Generates or retrieves a cached BitMatrix from raw string content.
     */
    fun generateBitMatrix(
        content: String,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): BitMatrix? {
        if (content.isEmpty()) return null
        val cacheKey = "${errorCorrectionLevel.name}_$content"
        bitMatrixCache.get(cacheKey)?.let { return it }

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel)
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            bitMatrixCache.put(cacheKey, bitMatrix)
            bitMatrix
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pre-generates and caches bit matrices for an entire list of QR stream chunks in background.
     */
    fun preloadChunks(
        chunks: List<String>,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ) {
        for (chunk in chunks) {
            generateBitMatrix(chunk, errorCorrectionLevel)
        }
    }

    /**
     * Generates a styled QR Code Bitmap from raw string content with customizable color scheme, module shape, and error correction.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        colorScheme: QrColorScheme = QrColorScheme.HIGH_CONTRAST_MONO,
        moduleShape: QrModuleShape = QrModuleShape.SQUARE,
        errorCorrectionLevel: QrErrorCorrectionLevel = QrErrorCorrectionLevel.LEVEL_M
    ): Bitmap? {
        if (content.isEmpty()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel.zxingLevel)
                put(EncodeHintType.MARGIN, 2)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val primaryColorInt = colorScheme.darkColor.toInt()
            val backgroundColorInt = colorScheme.lightColor.toInt()

            if (moduleShape == QrModuleShape.SQUARE) {
                val pixels = IntArray(matrixWidth * matrixHeight)
                for (y in 0 until matrixHeight) {
                    val offset = y * matrixWidth
                    for (x in 0 until matrixWidth) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) primaryColorInt else backgroundColorInt
                    }
                }
                val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
                bitmap
            } else {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(backgroundColorInt)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColorInt
                    style = Paint.Style.FILL
                }

                val moduleWidth = size.toFloat() / matrixWidth
                val moduleHeight = size.toFloat() / matrixHeight
                val cornerRadius = moduleWidth * moduleShape.cornerRadiusFraction

                val rect = RectF()
                for (y in 0 until matrixHeight) {
                    for (x in 0 until matrixWidth) {
                        if (bitMatrix.get(x, y)) {
                            rect.set(
                                x * moduleWidth,
                                y * moduleHeight,
                                (x + 1) * moduleWidth,
                                (y + 1) * moduleHeight
                            )
                            if (moduleShape == QrModuleShape.DOTS) {
                                val radius = minOf(moduleWidth, moduleHeight) * 0.45f
                                canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint)
                            } else {
                                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                            }
                        }
                    }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
