package com.budgetmaster.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Google's "G", as the sign-in button is required to show it.
 *
 * Drawn in Kotlin rather than shipped as a drawable because the four brand colours must survive
 * intact on Android, iOS and the web, and a vector built here needs no resource conversion on any
 * of them. It is also the one place in the app where hardcoded colours are correct: these are
 * Google's, fixed by their branding terms, and tinting them with the app's palette — which is what
 * a theme-aware icon would do — is precisely what those terms forbid.
 */
val GoogleLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 " +
                    "3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z",
            ),
            fill = SolidColor(Color(0xFF4285F4)),
        )
        addPath(
            pathData = addPathNodes(
                "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 " +
                    "1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z",
            ),
            fill = SolidColor(Color(0xFF34A853)),
        )
        addPath(
            pathData = addPathNodes(
                "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 " +
                    "1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z",
            ),
            fill = SolidColor(Color(0xFFFBBC05)),
        )
        addPath(
            pathData = addPathNodes(
                "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 " +
                    "3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z",
            ),
            fill = SolidColor(Color(0xFFEA4335)),
        )
    }.build()
}
