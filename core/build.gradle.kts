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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        androidResources {
            enable = true
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
            implementation(libs.kotlinx.coroutines.core)
            
            // Compose Multiplatform for resources + design system foundation
            implementation(compose.runtime)
            implementation(compose.components.resources)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
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

