package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

/**
 * Creates the SQLDelight sql.js web worker (bundled from
 * `@cashapp/sqldelight-sqljs-worker`); the `sql-wasm.wasm` binary is copied to the
 * bundle root by `webApp/webpack.config.d/sqljs-config.js`.
 *
 * Top-level function because Kotlin/Wasm requires `js()` to be the whole body.
 */
private fun createSqlJsWorker(): Worker =
    js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")

/**
 * WasmJs implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        val driver = WebWorkerDriver(createSqlJsWorker())
        // The sql.js worker starts with an empty in-memory database on every page
        // load, so the schema must be created here (Android/iOS drivers do this
        // internally via their schema callbacks).
        BudgetMasterDatabase.Schema.awaitCreate(driver)
        return driver
    }
}
