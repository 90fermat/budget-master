package com.budgetmaster.core.ocr

/**
 * No on-device OCR wired here, so receipt scan hides itself rather than pretending.
 *
 * ML Kit is a native Android/iOS SDK with no Kotlin Multiplatform wrapper; wiring it would mean
 * bridging the native SDK per platform. Sending the raw photo to a cloud OCR instead was rejected:
 * a receipt image carries far more than the total (card digits, address, the full basket), and it
 * should stay on the device.
 */
internal class UnavailableReceiptTextRecognizer : ReceiptTextRecognizer {
    override val isAvailable: Boolean = false
    override suspend fun recognizeText(image: ReceiptImage): String? = null
}

actual fun createReceiptTextRecognizer(): ReceiptTextRecognizer = UnavailableReceiptTextRecognizer()
