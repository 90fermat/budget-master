package com.budgetmaster.auth.di

import com.budgetmaster.auth.data.repository.WasmAuthRepository
import com.budgetmaster.auth.domain.repository.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * WasmJs implementation of [platformAuthModule] containing the mock WasmAuthRepository binding.
 */
actual val platformAuthModule: Module = module {
    single { WasmAuthRepository(get()) } bind AuthRepository::class
}
