@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.budgetmaster.reports.domain

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** Writes the CSV to a temp file and presents a `UIActivityViewController`. */
actual suspend fun shareCsv(fileName: String, content: String): Boolean {
    val path = NSTemporaryDirectory() + fileName
    // Was `(content as NSString).writeToFile(...)`, which the compiler now correctly rejects as a
    // cast that can never succeed: a Kotlin String is not an instance of the Objective-C NSString
    // class. Bridging happens implicitly when a value crosses an interop boundary, never for an
    // explicit Kotlin-side cast, so this would have thrown the moment an iOS user exported a CSV.
    //
    // Going through NSData sidesteps the question entirely and pins the encoding to UTF-8, which
    // is what the CSV claims to be.
    val bytes = content.encodeToByteArray()
    val data = if (bytes.isEmpty()) {
        NSData()
    } else {
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }
    val written = data.writeToFile(path, atomically = true)
    if (!written) return false

    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    val controller = UIActivityViewController(
        activityItems = listOf(NSURL.fileURLWithPath(path)),
        applicationActivities = null,
    )
    root.presentViewController(controller, animated = true, completion = null)
    return true
}
