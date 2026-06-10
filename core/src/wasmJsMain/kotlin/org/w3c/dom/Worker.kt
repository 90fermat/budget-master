package org.w3c.dom

/**
 * Stub for Web Worker in WasmJs (removed from stdlib in Kotlin 2.1.0).
 * Required by SQLDelight web-worker-driver.
 */
external class Worker(scriptURL: String) {
    fun postMessage(message: JsAny?)
    fun terminate()
}
