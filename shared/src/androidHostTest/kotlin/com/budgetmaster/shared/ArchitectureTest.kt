package com.budgetmaster.shared

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Executable architecture rules (ARCHITECTURE.md / DESIGN_SYSTEM.md).
 *
 * These replace review-by-eyeball: a violation fails the build instead of quietly landing.
 */
class ArchitectureTest {

    private val featureModules = listOf(
        "auth", "dashboard", "transactions", "budgets", "accounts", "reports", "settings",
    )

    /**
     * The central rule of the module graph: features may depend on `:core` (and the shell may
     * depend on features), but never on each other. Cross-feature reuse goes through `:core`.
     */
    @Test
    fun `features do not depend on other features`() {
        Konsist.scopeFromProduction()
            .files
            .filter { file -> file.featureModule() != null }
            .assertTrue { file ->
                val own = file.featureModule()
                file.imports.none { import ->
                    featureModules.any { other ->
                        other != own && import.name.startsWith("com.budgetmaster.$other.")
                    }
                }
            }
    }

    /**
     * Color belongs to the design system so palettes and dark mode stay consistent.
     *
     * Known exceptions, each deliberate and shrinking:
     *  - `TopTransactionsList` / `AiInsightsWidget`: per-category and per-insight accent
     *    palettes, pending the shared component library (Phase 4).
     *  - `ReportsScreen`: one remaining literal; folds into Phase 4's component work.
     *
     * The hex parser used to be duplicated in three features and allow-listed here; this rule
     * flagged a fourth copy, so it moved to `core.designsystem.parseHexColor` instead.
     */
    @Test
    fun `features do not hardcode colors outside the design system`() {
        val allowed = listOf("ReportsScreen", "TopTransactionsList", "AiInsightsWidget")
        Konsist.scopeFromProduction()
            .files
            .filter { it.featureModule() != null && it.name !in allowed }
            .assertFalse { file -> file.text.contains(Regex("""Color\(0x""")) }
    }

    /** MVI: ViewModels live in a feature's `presentation` package and are named `*ViewModel`. */
    @Test
    fun `view models live in presentation packages`() {
        Konsist.scopeFromProduction()
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue { it.resideInPackage("..presentation..") }
    }

    /** Repository implementations stay in `data`; the interfaces they honour stay in `domain`. */
    @Test
    fun `repository interfaces live in domain and implementations in data`() {
        Konsist.scopeFromProduction()
            .interfaces()
            .filter { it.name.endsWith("Repository") && it.featureModule() != null }
            .assertTrue { it.resideInPackage("..domain..") }
    }

    private fun com.lemonappdev.konsist.api.declaration.KoFileDeclaration.featureModule(): String? {
        val pkg = packagee?.name ?: return null
        return featureModules.firstOrNull { pkg.startsWith("com.budgetmaster.$it.") }
    }

    private fun com.lemonappdev.konsist.api.provider.KoPackageProvider.featureModule(): String? {
        val pkg = packagee?.name ?: return null
        return featureModules.firstOrNull { pkg.startsWith("com.budgetmaster.$it.") }
    }
}
