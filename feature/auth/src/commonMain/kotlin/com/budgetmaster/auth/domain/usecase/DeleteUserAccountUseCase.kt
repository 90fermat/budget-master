package com.budgetmaster.auth.domain.usecase

import com.budgetmaster.auth.domain.repository.AuthRepository
import com.budgetmaster.core.db.UserDataEraser
import com.budgetmaster.core.session.SessionStore

/**
 * Permanently deletes the signed-in user's account: the auth credential **and** all their local
 * data.
 *
 * Order is deliberate. The remote credential is deleted **first**; only if that succeeds is the
 * local ledger wiped. Doing it the other way round would, on a re-authentication failure, leave
 * the user still able to sign in but with an empty app — the worst outcome. If the provider needs
 * a recent login it throws here, the local data is untouched, and the caller asks the user to
 * sign in again and retry.
 *
 * Play requires this path for any app that has accounts, and "delete my account" must mean the
 * data is gone, not merely a sign-out.
 */
class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val userDataEraser: UserDataEraser,
    private val sessionStore: SessionStore,
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        val userId = sessionStore.currentUserId.value

        // Remote first: if this throws (e.g. requires-recent-login), we stop here with local data
        // intact.
        authRepository.deleteAccount()

        // Then the local ledger. A null id means the local-default profile, which is what the
        // eraser is given so its rows go too.
        userId?.let { userDataEraser.eraseAllData(it) }

        sessionStore.setCurrentUser(null)
    }
}
