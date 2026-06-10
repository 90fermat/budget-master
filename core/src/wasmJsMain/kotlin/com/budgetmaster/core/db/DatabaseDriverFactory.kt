package com.budgetmaster.core.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * WasmJs implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        // Instantiate the Web Worker with the worker script path
        val worker = Worker("sqlite.worker.js")
        return WebWorkerDriver(worker)
    }
}
