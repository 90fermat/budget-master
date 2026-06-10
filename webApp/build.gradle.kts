plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "webApp"
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.koin.core)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
    }
}
