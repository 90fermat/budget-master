import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.roborazzi)
}

// ── Version ──────────────────────────────────────────────────────────────────
// Derived from the most recent `v*` git tag (e.g. v1.4.2 -> versionName 1.4.2, versionCode
// 10402) so a release is named by the tag it was cut from rather than a number someone has to
// remember to bump. Falls back to 0.1.0/1 in a shallow clone or a tree with no tags, which is
// why CI release lanes must fetch tags.
// isIgnoreExitValue matters: `git describe` exits 128 when no tag matches, which is the case in
// a fresh clone, a shallow CI checkout, and this repo today. Without it every build fails rather
// than falling back.
val gitDescribe = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0", "--match=v*")
    isIgnoreExitValue = true
}

val gitVersionName: String = runCatching {
    gitDescribe.standardOutput.asText.get().trim().removePrefix("v")
}.getOrDefault("").ifBlank { "0.1.0" }

val gitVersionCode: Int = gitVersionName.split(".").let { parts ->
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    // Monotonic as long as minor/patch stay under 100, which the scheme assumes.
    (major * 10000) + (minor * 100) + patch
}.coerceAtLeast(1)

// ── Signing ──────────────────────────────────────────────────────────────────
// Read from keystore.properties (gitignored) or the matching environment variables in CI. No
// keystore, password, or alias is ever committed; without them the release build simply stays
// unsigned rather than failing, so `assembleRelease` still works for anyone checking the build.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_FILE")
val hasReleaseSigning = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

// AGP 9: Kotlin support is built in — no org.jetbrains.kotlin.android plugin.
// This module is a thin Android entry point; all shared UI lives in :shared.
android {
    namespace = "com.budgetmaster.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.budgetmaster"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
        // For BuildConfig.DEBUG, which selects the App Check provider (debug token vs Play
        // Integrity). AGP does not generate BuildConfig unless asked.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/module-info.class"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // No applicationIdSuffix on debug: google-services.json only declares a client for
        // com.budgetmaster, and the registered SHA-1 is bound to it, so suffixing the debug id
        // would fail the google-services plugin and break Google sign-in.
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

    // App Check, required by Firebase AI Logic. Play Integrity attests real release installs;
    // debug builds use a token registered in the console (see BudgetMasterApplication).
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
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
