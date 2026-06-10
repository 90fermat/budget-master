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
    }
}
