package com.budgetmaster.core.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Android OCR via ML Kit's on-device Latin text recognizer.
 *
 * Bundled (not Play-Services-downloaded) so it works on first launch with no model download and
 * no network — the whole point is that the receipt photo never leaves the device.
 */
internal class MlKitReceiptTextRecognizer : ReceiptTextRecognizer {

    override val isAvailable: Boolean = true

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeText(image: ReceiptImage): String? {
        val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
            ?: return null // Undecodable bytes are an unreadable photo, not a crash.
        return try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            result.text.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }
}

actual fun createReceiptTextRecognizer(): ReceiptTextRecognizer = MlKitReceiptTextRecognizer()
