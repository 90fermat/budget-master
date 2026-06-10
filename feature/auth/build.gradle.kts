plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
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
            baseName = "auth"
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
            
            // Compose Multiplatform Core
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // Navigation
            implementation(libs.navigation.compose)
            
            // Icons
            implementation(compose.materialIconsExtended)
            
            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }
        
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            
            // Android Biometrics support
            implementation(libs.androidx.biometric)
        }
        
        iosMain.dependencies {
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        }
    }
}

android {
    namespace = "com.budgetmaster.auth"
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
