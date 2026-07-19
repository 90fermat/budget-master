package com.budgetmaster.android

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.budgetmaster.shared.App
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.shared.MoneyMessageImporter
import com.budgetmaster.auth.util.ActivityProvider
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.android.ext.android.inject

/**
 * Main Activity for the Android application.
 */
class MainActivity : FragmentActivity() {

    private val importer: MoneyMessageImporter by inject()
    private val settingsRepository: AppSettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.setActivity(this)
        applySecureScreenSetting()
        setContent {
            App()
        }
        handleSharedText(intent)
    }

    /**
     * Keeps FLAG_SECURE in step with the user's preference.
     *
     * FLAG_SECURE is what stops the window being captured: no screenshots, no screen recording,
     * and - the case that actually matters day to day - no live thumbnail of the user's balances
     * in the recents switcher. It has to be a window flag rather than anything in Compose,
     * because the recents snapshot is taken by the system outside the app's own drawing.
     *
     * Collected for the whole lifetime of the activity so toggling it in Settings takes effect
     * immediately rather than on next launch.
     */
    private fun applySecureScreenSetting() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                settingsRepository.settings
                    .map { it.secureScreen }
                    .distinctUntilChanged()
                    .collect { secure ->
                        if (secure) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedText(intent)
    }

    /**
     * Imports a mobile-money message shared in from the SMS app.
     *
     * The fallback capture path — for a refused SMS permission, or a message the receiver missed.
     * It goes through the same importer as automatic capture, so sharing something already
     * captured is a no-op rather than a duplicate.
     */
    private fun handleSharedText(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return
        // Consumed: a config change would otherwise re-deliver and re-import the same share.
        intent.removeExtra(Intent.EXTRA_TEXT)

        lifecycleScope.launch {
            val outcome = importer.import(sender = "", body = text, receivedAt = System.currentTimeMillis())
            val message = when (outcome) {
                is ImportOutcome.Imported -> getString(R.string.share_import_ok)
                // Not "skipped": the message was read, and the decision is now waiting in the
                // review queue on the Transactions screen.
                is ImportOutcome.NeedsReview -> getString(R.string.share_import_review)
                null -> getString(R.string.share_import_disabled)
                else -> getString(R.string.share_import_skipped)
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityProvider.getActivity() == this) {
            ActivityProvider.setActivity(null)
        }
    }
}
