plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.roborazzi)
}

// AGP 9: Kotlin support is built in — no org.jetbrains.kotlin.android plugin.
// This module is a thin Android entry point; all shared UI lives in :shared.
android {
    namespace = "com.budgetmaster.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.budgetmaster"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/module-info.class"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    lint {
        abortOnError = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:transactions"))
    implementation(libs.koin.android)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    debugImplementation(libs.ui.tooling)

    // Screenshot tests (Roborazzi + Robolectric) for shared Compose UI.
    testImplementation(project(":feature:dashboard"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    // Jetpack Compose test artifacts matching CMP 1.11.1 (maps to Jetpack Compose 1.11.2)
    testImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation so the ComponentActivity test host is merged into the debug manifest
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.kotlinx.datetime)
}
