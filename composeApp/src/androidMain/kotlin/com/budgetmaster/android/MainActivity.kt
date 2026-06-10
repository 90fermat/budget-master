package com.budgetmaster.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.budgetmaster.shared.App
import com.budgetmaster.auth.util.ActivityProvider

/**
 * Main Activity for the Android application.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.setActivity(this)
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityProvider.getActivity() == this) {
            ActivityProvider.setActivity(null)
        }
    }
}
