package com.budgetmaster.android

import android.app.Activity
import android.os.Bundle
import com.budgetmaster.core.security.AppLockController
import com.budgetmaster.core.sync.SyncController
import android.app.Application
import com.budgetmaster.core.config.RemoteFeatureFlags
import com.budgetmaster.core.db.AppContextHolder
import com.budgetmaster.shared.di.initKoin
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.perf.performance
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android Application class for setting up configurations and dependency injection.
 */
class BudgetMasterApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()

        // Initialize static Context holder for SQLDelight drivers
        AppContextHolder.context = this

        initializeAppCheck()
        initializeCrashReporting()

        // Initialize Koin DI
        initKoin {
            androidContext(this@BudgetMasterApplication)
            modules(androidAppModule)
        }

        // Daily background pass so recurring entries appear on their own day instead of
        // waiting for the next launch. Must come after Koin: the worker injects from it.
        RecurringWorker.schedule(this)

        // Pull the latest remote feature flags (AI kill-switch) in the background. Fire-and-forget
        // by design: the cached/default values are already live, so a slow or failed fetch never
        // holds up start.
        val remoteFlags: RemoteFeatureFlags by inject()
        CoroutineScope(Dispatchers.Default).launch { remoteFlags.refresh() }

        observeForegroundForAppLock()
    }

    /**
     * Tells the app lock when the app leaves and returns to the foreground.
     *
     * Counted across activities rather than hooked to a single one, so rotating the screen or
     * opening a second activity is not mistaken for leaving the app - the count only reaches zero
     * when nothing of ours is on screen.
     *
     * Activity callbacks rather than ProcessLifecycleOwner: the behaviour needed here is exactly
     * this counter, and it needs no extra dependency to get it.
     */
    private fun observeForegroundForAppLock() {
        val appLock: AppLockController by inject()
        val sync: SyncController by inject()
        registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private var startedActivities = 0

                override fun onActivityStarted(activity: Activity) {
                    if (startedActivities == 0) {
                        appLock.onMovedToForeground()
                        // Coming back to the app is the moment another device's changes are most
                        // likely to be waiting, and the only one the user is actually present for.
                        // A no-op when signed out or when there is nothing to exchange.
                        sync.requestSync()
                    }
                    startedActivities++
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities--
                    if (startedActivities == 0) appLock.onMovedToBackground()
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    /**
     * App Check attests that requests come from this genuine app build.
     *
     * Firebase AI Logic enforces it (Google began auto-enforcing for AI Logic in early July
     * 2026), so without a provider the insights calls are rejected — this is not optional
     * hardening, it is what makes the feature work at all.
     *
     * The provider comes from the variant's source set — debug token vs Play Integrity — because
     * the debug App Check artifact is a `debugImplementation` dependency and referencing it from
     * here would compile in debug and break the release build.
     */
    private fun initializeAppCheck() {
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
        // Without this, refresh follows the global data-collection default and a token that
        // expires mid-session is not renewed until something already failed.
        Firebase.appCheck.setTokenAutoRefreshEnabled(true)
    }

    /**
     * Crash reporting, off in debug builds.
     *
     * A crash while someone is developing is not a signal about the shipped app, and letting
     * those through buries the real reports. Crashlytics collects stack traces and device
     * metadata only — nothing here ever logs a transaction, an amount, or an email, and it must
     * stay that way: this is a finance app, and a crash report is not a place for the ledger.
     */
    private fun initializeCrashReporting() {
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        // Same reasoning: a developer's cold start on a debug build, behind a debugger, is not a
        // signal about the shipped app — it is noise that drags the percentiles around.
        Firebase.performance.isPerformanceCollectionEnabled = !BuildConfig.DEBUG
    }
}
