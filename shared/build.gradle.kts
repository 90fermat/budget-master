plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
            implementation(project(":feature:reports"))
            implementation(project(":feature:settings"))
            
            // Compose Multiplatform Core
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Material3 Adaptive components
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)
            
            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // Navigation
            implementation(libs.navigation.compose)
            
            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }
        
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.vico.core)
            
            // Android Biometrics support
            implementation(libs.androidx.biometric)
        }
        
        iosMain.dependencies {
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
        }
        
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
        }
    }
}

android {
    namespace = "com.budgetmaster.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "/META-INF/versions/9/module-info.class"
        }
    }
}
