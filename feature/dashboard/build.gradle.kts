plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.budgetmaster.dashboard"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
            baseName = "dashboard"
            isStatic = true
        }
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    applyDefaultHierarchyTemplate()
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotlinx.datetime)
            
            // Compose Multiplatform Core
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // Navigation
            implementation(libs.navigation.compose)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        
        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.vico.core)
        }
        
        // iOS and wasmJs draw the chart with Compose Canvas and need no charting dependency:
        // Vico publishes Android artifacts only.

        val wasmJsMain by getting {
            dependencies {
                // Wasm-specific dependencies if any
            }
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
            }
        }
    }
}

// The GEMINI_API_KEY BuildConfig generation that used to live here is gone: insights now go
// through Firebase AI Logic (`core.ai.GenAiClient`), which proxies Gemini and attests the caller
// with App Check, so there is no key to inject, guard against embedding in release builds, or
// keep out of the repo. The safest secret is the one that doesn't exist.

