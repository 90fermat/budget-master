plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.budgetmaster.core"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            // 17 rather than 11 across every module, because the Firebase KMP SDK's Firestore
            // artifact ships JVM 17 bytecode and its API is largely `inline` — inlining it into an
            // 11-target compilation is a hard error. Raising only this module would move the
            // problem rather than solve it: anything inlining from :core would hit the same wall,
            // and it would surface later and further from the cause. D8 desugars to DEX, so
            // minSdk 26 is unaffected.
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
        withHostTest {
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "core"
            isStatic = true
        }
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    applyDefaultHierarchyTemplate()

    sourceSets {
        // A `mobile` source set shared by Android and iOS, holding sync's Firestore binding: it is
        // genuinely one implementation for both, since the Firebase KMP SDK exposes the same API on
        // each, and keeping it in androidMain would make iOS support a rewrite rather than a Koin
        // binding. Wasm is excluded deliberately — its database is recreated on every page load, so
        // there is no local state for sync to reconcile.
        //
        // Wired by hand rather than through the hierarchy template's `group("mobile")`, because
        // `withAndroidTarget()` matches the old `androidTarget()` and not the target this module
        // declares via the Android KMP library plugin. The template silently left Android out, and
        // the source set compiled under metadata while never reaching the Android artifact — code
        // that appears to build and simply is not there. Verified with a deliberate type error.
        val mobileMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // The Firebase KMP SDK, the same one :feature:auth already uses, so sync speaks to
                // Firestore through one API on both platforms.
                implementation(libs.firebase.common)
                implementation(libs.firebase.firestore)
            }
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

        commonMain.dependencies {
            implementation(libs.koin.core)
            // koinInject in the shared guidance composables
            implementation(libs.koin.compose)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            
            // Compose Multiplatform for resources + design system foundation
            implementation(compose.runtime)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            // Category icons (vectors render everywhere; emoji are tofu on Wasm).
            implementation(compose.materialIconsExtended)
        }
        
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.datastore.preferences)
            // Firebase AI Logic: Android-only, so iOS/Wasm get the unavailable GenAiClient.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.ai)
            implementation(libs.firebase.config)
            // On-device receipt OCR; the photo never leaves the phone.
            implementation(libs.mlkit.text.recognition)
            // rememberLauncherForActivityResult, for the photo picker and SMS permission.
            implementation(libs.androidx.activity.compose)
            // BiometricPrompt for the app lock. Lives here rather than in :feature:auth because
            // app lock is a core security concern, and :core cannot depend on a feature.
            implementation(libs.androidx.biometric)
            implementation("androidx.core:core-ktx:1.15.0")
        }
        
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.datastore.preferences)
        }

        
        val androidHostTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.driver.sqlite)
                // Scripts the exchange-rate endpoint without touching the network.
                implementation(libs.ktor.client.mock)
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.sqldelight.driver.webworker)
            // Official browser API externals (org.w3c.dom.Worker etc.) for Kotlin/Wasm
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            // SQLDelight sql.js web worker (runs SQLite in a background worker)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.3.2"))
            implementation(npm("sql.js", "1.14.1"))
        }
    }
}


sqldelight {
    databases {
        create("BudgetMasterDatabase") {
            packageName.set("com.budgetmaster.core.db")
            generateAsync.set(true)
        }
    }
}

compose.resources {
    publicResClass = true
}

