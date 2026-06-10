package com.budgetmaster.auth.util

import android.app.Activity

/**
 * Provides the current Android [Activity] context for biometric prompts.
 * Must be updated from the host Activity's lifecycle.
 */
object ActivityProvider {
    private var activity: Activity? = null

    /** Sets the current [Activity] reference. */
    fun setActivity(activity: Activity?) {
        this.activity = activity
    }

    /** Returns the current [Activity], or null if none is set. */
    fun getActivity(): Activity? = activity
}
