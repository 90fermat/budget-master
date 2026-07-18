package com.budgetmaster.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.budgetmaster.shared.App
import com.budgetmaster.shared.MoneyMessageImporter
import com.budgetmaster.auth.util.ActivityProvider
import com.budgetmaster.transactions.domain.usecase.ImportOutcome
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Main Activity for the Android application.
 */
class MainActivity : FragmentActivity() {

    private val importer: MoneyMessageImporter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.setActivity(this)
        setContent {
            App()
        }
        handleSharedText(intent)
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
