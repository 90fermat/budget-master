package com.budgetmaster.core.security

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * The Activity currently in the foreground, for APIs that need one rather than a Context.
 *
 * [BiometricPrompter] needs a `FragmentActivity` to host the system sheet. Held weakly and
 * cleared on destroy, because a static strong reference to an Activity leaks the whole view
 * hierarchy for the life of the process.
 */
object CurrentActivityHolder {
    private var ref: WeakReference<Activity>? = null

    var activity: Activity?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
