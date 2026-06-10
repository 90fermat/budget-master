package com.budgetmaster.core.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * WasmJs implementation of [platformCoreModule] (empty since WasmJs preferences are mocked/unsupported).
 */
actual val platformCoreModule: Module = module {}
