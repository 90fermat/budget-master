plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            
            // Compose Multiplatform for resources
            implementation(compose.runtime)
            implementation(compose.components.resources)
        }
        
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.datastore.preferences)
        }
        
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.datastore.preferences)
        }
        
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.driver.webworker)
        }
    }
}

android {
    namespace = "com.budgetmaster.core"
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

