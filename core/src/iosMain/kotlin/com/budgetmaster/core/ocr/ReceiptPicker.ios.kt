package com.budgetmaster.core.ocr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * No picker wired here, so the scan action stays hidden. The OCR step is unavailable on this
 * platform anyway (see ReceiptTextRecognizer), so a picker alone would lead nowhere.
 */
@Composable
actual fun rememberReceiptPicker(onImage: (ReceiptImage) -> Unit): ReceiptPicker = remember {
    object : ReceiptPicker {
        override val isSupported = false
        override fun launch() = Unit
    }
}
