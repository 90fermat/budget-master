package com.budgetmaster.core.ocr

import androidx.compose.runtime.Composable

/**
 * Lets the user supply a receipt photo.
 *
 * Uses the system **photo picker** rather than an in-app camera: it needs no camera or storage
 * permission at all (the user hands over exactly one image), gives full resolution — a camera
 * *preview* thumbnail is far too low-res to OCR — and covers both "photograph it now" and "that
 * receipt I shot yesterday". Fewer permissions is also a better data-safety answer on Play.
 */
interface ReceiptPicker {
    /** False where no picker is wired; callers hide the scan action. */
    val isSupported: Boolean

    /** Opens the picker. The result arrives on the callback passed to [rememberReceiptPicker]. */
    fun launch()
}

/**
 * Remembers a [ReceiptPicker] that delivers the chosen image to [onImage].
 *
 * @param onImage called with the encoded bytes when the user picks an image; not called if they
 *   cancel.
 */
@Composable
expect fun rememberReceiptPicker(onImage: (ReceiptImage) -> Unit): ReceiptPicker
