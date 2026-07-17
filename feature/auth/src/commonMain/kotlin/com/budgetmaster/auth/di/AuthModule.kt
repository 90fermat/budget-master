package com.budgetmaster.auth.di

import com.budgetmaster.auth.domain.usecase.CheckAuthStatusUseCase
import com.budgetmaster.auth.domain.usecase.CheckBiometricSupportUseCase
import com.budgetmaster.auth.domain.usecase.CheckFirstLaunchUseCase
import com.budgetmaster.auth.domain.usecase.CompleteOnboardingUseCase
import com.budgetmaster.auth.domain.usecase.GetCurrentUserUseCase
import com.budgetmaster.auth.domain.usecase.LoginUseCase
import com.budgetmaster.auth.domain.usecase.ResetPasswordUseCase
import com.budgetmaster.auth.domain.usecase.SignInUseCase
import com.budgetmaster.auth.domain.usecase.SignInWithGoogleUseCase
import com.budgetmaster.auth.domain.usecase.DeleteAccountUseCase
import com.budgetmaster.auth.domain.usecase.SignOutUseCase
import com.budgetmaster.auth.domain.usecase.SignUpUseCase
import com.budgetmaster.auth.domain.usecase.ToggleBiometricUseCase
import com.budgetmaster.auth.presentation.biometric.BiometricViewModel
import com.budgetmaster.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.budgetmaster.auth.presentation.login.LoginViewModel
import com.budgetmaster.auth.presentation.onboarding.OnboardingViewModel
import com.budgetmaster.auth.presentation.register.RegisterViewModel
import com.budgetmaster.auth.presentation.splash.SplashViewModel
import com.budgetmaster.auth.util.BiometricAuthenticator
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module declaring dependency injection bindings for the Auth and Onboarding features.
 */
val authModule = module {
    // Platform utilities
    single { BiometricAuthenticator() }

    // Use cases
    factory { GetCurrentUserUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { DeleteAccountUseCase(get(), get(), get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { CheckBiometricSupportUseCase(get()) }
    factory { ToggleBiometricUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { SignInWithGoogleUseCase(get()) }
    factory { CheckAuthStatusUseCase(get()) }
    factory { CheckFirstLaunchUseCase(get()) }
    factory { CompleteOnboardingUseCase(get()) }

    // ViewModels
    viewModel { SplashViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { BiometricViewModel(get(), get(), get()) }
}

/**
 * Platform-specific module containing authentication and Firebase client bindings.
 */
expect val platformAuthModule: Module
