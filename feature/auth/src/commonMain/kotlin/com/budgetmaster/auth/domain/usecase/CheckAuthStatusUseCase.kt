package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe whether the user is authenticated.
 */
class CheckAuthStatusUseCase(private val authRepository: AuthRepository) {
    /**
     * Returns a flow emitting the current [AuthStatus].
     */
    operator fun invoke(): Flow<AuthStatus> = authRepository.getAuthStatus()
}
