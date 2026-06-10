package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the current authenticated user status.
 */
class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    /**
     * Returns a flow that emits the current [AuthStatus] whenever it changes.
     */
    operator fun invoke(): Flow<AuthStatus> = authRepository.getAuthStatus()
}
