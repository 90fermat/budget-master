package com.budgetmaster.reports.domain

/**
 * Hands a generated CSV to the platform: a share sheet on Android/iOS, a file download on Web.
 *
 * @return true when the platform accepted it; false if sharing isn't possible, so the UI can
 * say so rather than appear to succeed.
 */
expect suspend fun shareCsv(fileName: String, content: String): Boolean
