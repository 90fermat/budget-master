package com.budgetmaster.core.ocr

/**
 * On-device text recognition for a captured receipt image.
 *
 * Deliberately **on-device**: ML Kit runs the OCR locally and free, so the photograph of a receipt
 * — which shows a card's last digits, an address, a full itemised basket — never leaves the phone.
 * Only the short text the parser needs is later summarised to the model.
 *
 * @see ReceiptImage for why the image crosses this boundary as bytes.
 */
interface ReceiptTextRecognizer {
    /** Whether OCR is available on this platform/build. */
    val isAvailable: Boolean

    /**
     * Extracts the visible text from [image].
     *
     * @return the recognised text, or null if nothing could be read (a blurry photo, no text).
     *   Never throws for an unreadable image — that is an outcome, not an error.
     */
    suspend fun recognizeText(image: ReceiptImage): String?
}

/**
 * A captured image as encoded bytes (JPEG/PNG).
 *
 * Bytes rather than a platform bitmap type so the interface stays in `commonMain`; each actual
 * decodes them with its own toolkit.
 */
data class ReceiptImage(val bytes: ByteArray) {
    // ByteArray uses identity equals, which would make two identical captures unequal and break
    // any state comparison this ends up inside.
    override fun equals(other: Any?): Boolean =
        this === other || (other is ReceiptImage && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = bytes.contentHashCode()
}

/** The platform's recognizer: ML Kit on Android, unavailable elsewhere. */
expect fun createReceiptTextRecognizer(): ReceiptTextRecognizer
