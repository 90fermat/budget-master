plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // For @Serializable on the AI quick-add response DTO.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.budgetmaster.transactions"
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
            baseName = "transactions"
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
            implementation(libs.kotlinx.coroutines.core)
            // Parses the AI quick-add JSON response.
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

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
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {}

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
            }
        }
    }
}

