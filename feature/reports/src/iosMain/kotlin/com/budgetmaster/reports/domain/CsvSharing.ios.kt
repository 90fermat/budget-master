@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.budgetmaster.reports.domain

import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/** Writes the CSV to a temp file and presents a `UIActivityViewController`. */
actual suspend fun shareCsv(fileName: String, content: String): Boolean {
    val path = NSTemporaryDirectory() + fileName
    val written = (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    if (!written) return false

    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    val controller = UIActivityViewController(
        activityItems = listOf(NSURL.fileURLWithPath(path)),
        applicationActivities = null,
    )
    root.presentViewController(controller, animated = true, completion = null)
    return true
}
