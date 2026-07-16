package com.budgetmaster.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.budgetmaster.transactions.domain.usecase.MaterializeDueRecurringUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Creates due recurring transactions in the background, so a scheduled entry lands on its own
 * day rather than whenever the app is next opened.
 *
 * This does not replace the catch-up on app start — it narrows the window. The two can't
 * conflict: `materializeDue()` gives each occurrence a deterministic id, so running it from
 * both places creates nothing twice.
 *
 * Android-only by design. iOS background execution is far more restrictive and the Web build
 * has no background at all; both keep open-time catch-up, which is still correct — entries
 * carry their true occurrence dates regardless of when they were materialized.
 */
class RecurringWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val materializeDue: MaterializeDueRecurringUseCase by inject()

    override suspend fun doWork(): Result = runCatching { materializeDue() }
        .fold(
            onSuccess = { Result.success() },
            // Retry rather than fail: a transient database error shouldn't skip a day's entries.
            onFailure = { Result.retry() },
        )

    companion object {
        private const val WORK_NAME = "recurring-materialize"

        /**
         * Schedules a daily pass, keeping any existing schedule so app restarts don't reset the
         * cycle. Requires the battery not to be low — creating ledger entries can wait a few
         * hours, and it is never worth a user's last few percent.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
