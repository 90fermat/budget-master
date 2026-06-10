plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":core"))
            implementation(project(":feature:auth"))
            implementation(libs.koin.android)
            implementation("androidx.activity:activity-compose:1.10.0")
            implementation("androidx.appcompat:appcompat:1.7.0")
        }
    }
}

android {
    namespace = "com.budgetmaster.android"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.budgetmaster"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/module-info.class"
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    
    lint {
        abortOnError = false
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
