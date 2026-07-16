package com.budgetmaster.android

import android.app.Application
import com.budgetmaster.core.db.AppContextHolder
import com.budgetmaster.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Android Application class for setting up configurations and dependency injection.
 */
class BudgetMasterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize static Context holder for SQLDelight drivers
        AppContextHolder.context = this
        
        // Initialize Koin DI
        initKoin {
            androidContext(this@BudgetMasterApplication)
        }

        // Daily background pass so recurring entries appear on their own day instead of
        // waiting for the next launch. Must come after Koin: the worker injects from it.
        RecurringWorker.schedule(this)
    }
}
