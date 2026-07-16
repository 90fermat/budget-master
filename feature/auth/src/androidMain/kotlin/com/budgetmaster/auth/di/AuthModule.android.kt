package com.budgetmaster.auth.di

import com.budgetmaster.auth.data.repository.FirebaseAuthRepository
import com.budgetmaster.auth.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Android implementation of [platformAuthModule] containing Firebase authentication bindings.
 */
actual val platformAuthModule: Module = module {
    single { Firebase.auth }
    single { FirebaseAuthRepository(get()) } bind AuthRepository::class
}
