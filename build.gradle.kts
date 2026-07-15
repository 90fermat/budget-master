import io.gitlab.arturbosch.detekt.Detekt

plugins {
    // Triggers compilation of KMP plugins
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.detekt)
}

subprojects {
    configurations.all {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
    }
}

// ── Static analysis + formatting ─────────────────────────────────────────────
// A single root `detekt` task scans all module Kotlin sources (no type resolution)
// with ktlint formatting rules bundled in. Reports for now (ignoreFailures = true);
// tighten to blocking after the initial findings are cleared (see ROADMAP.md).
dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = true
    parallel = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "core/src", "shared/src", "composeApp/src", "webApp/src",
            "feature/auth/src", "feature/dashboard/src", "feature/transactions/src",
            "feature/budgets/src", "feature/reports/src", "feature/settings/src",
        )
    )
}

tasks.withType<Detekt>().configureEach {
    exclude("**/build/**", "**/generated/**", "**/resources/**")
    reports {
        html.required.set(true)
        sarif.required.set(false)
        md.required.set(false)
        txt.required.set(false)
    }
}
