plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.budgetmaster.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
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
            baseName = "shared"
            isStatic = true
            // Export dependencies to Swift if needed
            export(project(":core"))
            export(project(":feature:auth"))
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
            implementation(project(":feature:auth"))
            implementation(project(":feature:dashboard"))
            implementation(project(":feature:transactions"))
            implementation(project(":feature:budgets"))
            implementation(project(":feature:accounts"))
            implementation(project(":feature:reports"))
            implementation(project(":feature:settings"))
            
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
            
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
        }
        
        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.common)
            implementation(libs.firebase.auth)
            
            
            // Android Biometrics support
            implementation(libs.androidx.biometric)
        }
        
        iosMain.dependencies {
            implementation(libs.firebase.common)
            implementation(libs.firebase.auth)
        }
        
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }

        // Konsist asserts architecture rules by scanning sources; it is JVM-only, so the
        // checks live in the Android host-test source set and cover the whole project.
        val androidHostTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.konsist)
            }
        }
    }
}

