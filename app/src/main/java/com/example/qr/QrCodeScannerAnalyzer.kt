package com.example.qr

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/**
 * CameraX ImageAnalysis analyzer optimized for rapid scanning of animated QR streams.
 */
class QrCodeScannerAnalyzer(
    private val onQrCodeDetected: (String) -> Unit,
    private val onMetricsUpdate: ((fps: Float, scanLatencyMs: Long) -> Unit)?
) : ImageAnalysis.Analyzer {

    constructor(onQrCodeDetected: (String) -> Unit) : this(onQrCodeDetected, null)

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }
        setHints(hints)
    }

    private var lastScannedText = ""
    private var lastScannedTimestamp = 0L
    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        frameCounter++

        if (startTime - lastFpsTimestamp >= 1000L) {
            val fps = (frameCounter * 1000f) / (startTime - lastFpsTimestamp)
            frameCounter = 0
            lastFpsTimestamp = startTime
            onMetricsUpdate?.invoke(fps, 0L)
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null && (imageProxy.format == ImageFormat.YUV_420_888 || imageProxy.format == ImageFormat.YUV_422_888 || imageProxy.format == ImageFormat.YUV_444_888)) {
            val planes = imageProxy.planes
            val yBuffer = planes[0].buffer // Y plane luminance
            val ySize = yBuffer.remaining()
            val yData = ByteArray(ySize)
            yBuffer.get(yData)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                val text = result.text
                val now = System.currentTimeMillis()
                val latency = now - startTime

                onMetricsUpdate?.invoke(0f, latency)

                // If same text, throttle to 180ms; if different text (e.g. animated stream next chunk), trigger immediately!
                if (text != lastScannedText || now - lastScannedTimestamp > 180) {
                    lastScannedText = text
                    lastScannedTimestamp = now
                    onQrCodeDetected(text)
                }
            } catch (_: NotFoundException) {
                // No QR code in current camera frame
            } catch (_: Exception) {
                // Ignore transient decode errors
            } finally {
                reader.reset()
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}

/**
 * CameraX ImageAnalysis analyzer linked directly to a QrStreamReassembler instance.
 */
class QrStreamScannerAnalyzer(
    private val reassembler: QrStreamReassembler,
    private val onChunkProcessed: ((ChunkProcessResult) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }
        setHints(hints)
    }

    private var lastScannedText = ""
    private var lastScannedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && (imageProxy.format == ImageFormat.YUV_420_888 || imageProxy.format == ImageFormat.YUV_422_888 || imageProxy.format == ImageFormat.YUV_444_888)) {
            val planes = imageProxy.planes
            val yBuffer = planes[0].buffer
            val ySize = yBuffer.remaining()
            val yData = ByteArray(ySize)
            yBuffer.get(yData)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                val text = result.text
                val now = System.currentTimeMillis()

                if (text != lastScannedText || now - lastScannedTimestamp > 180) {
                    lastScannedText = text
                    lastScannedTimestamp = now
                    val processResult = reassembler.processScannedQr(text)
                    onChunkProcessed?.invoke(processResult)
                }
            } catch (_: NotFoundException) {
                // No QR in frame
            } catch (_: Exception) {
                // Transient decode error
            } finally {
                reader.reset()
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}

object QrBitmapDecoder {
    fun decodeFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
            }
            setHints(hints)
        }

        return try {
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }
}

