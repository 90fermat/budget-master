package com.budgetmaster.android

import android.app.Application
import com.budgetmaster.core.db.AppContextHolder
import com.budgetmaster.shared.di.initKoin
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.Firebase
import org.koin.android.ext.koin.androidContext

/**
 * Android Application class for setting up configurations and dependency injection.
 */
class BudgetMasterApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize static Context holder for SQLDelight drivers
        AppContextHolder.context = this

        initializeAppCheck()

        // Initialize Koin DI
        initKoin {
            androidContext(this@BudgetMasterApplication)
        }

        // Daily background pass so recurring entries appear on their own day instead of
        // waiting for the next launch. Must come after Koin: the worker injects from it.
        RecurringWorker.schedule(this)
    }

    /**
     * App Check attests that requests come from this genuine app build.
     *
     * Firebase AI Logic enforces it (Google began auto-enforcing for AI Logic in early July
     * 2026), so without a provider the insights calls are rejected — this is not optional
     * hardening, it is what makes the feature work at all.
     *
     * Debug builds use the debug provider, whose token must be registered once per machine or
     * emulator under **App Check → Apps → Manage debug tokens** in the Firebase console; the
     * token is printed to logcat on first run. Release builds use Play Integrity, which needs no
     * setup beyond the app being distributed by Play.
     */
    private fun initializeAppCheck() {
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )
    }
}
