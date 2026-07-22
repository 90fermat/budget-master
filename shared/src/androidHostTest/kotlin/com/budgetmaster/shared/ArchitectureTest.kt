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
     * Packages that handle money, message text, or credentials.
     *
     * Everything in this app is arguably sensitive, but these are where a stray log line would be
     * worst: the SMS parsers see raw message bodies, the repositories see amounts and account
     * ids, and the AI services see prompts built from spending.
     */
    private val sensitivePackages = listOf(
        ".sms.", ".data.repository.", ".data.service.", ".ai.", ".auth.",
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
     * **No exceptions.** This rule carried an allowlist through Phases 0–3 and it is now
     * empty: the hex parser moved to `core.designsystem.parseHexColor`, category accents to
     * `categoryAccentFor`, and insight accents to `financialColors`/`colorScheme` tokens. A
     * literal in a feature means the design system is missing a token — add the token.
     */
    @Test
    fun `features do not hardcode colors outside the design system`() {
        Konsist.scopeFromProduction()
            .files
            .filter { it.featureModule() != null }
            .assertFalse { file -> file.text.contains(Regex("""Color\(0x""")) }
    }

    /**
     * Borders and surfaces must not be faded below usability.
     *
     * The dashboard drew 1dp borders at `outline.copy(alpha = 0.05f)` — mathematically present
     * and visually nothing — and tinted card containers at 15–20% `surfaceVariant`. The design
     * system already has the right tokens (`outlineVariant`, `surface`), which the newer
     * features use. This pins that: fading `outline` or `surfaceVariant` below 30% means
     * reaching for a token that already exists.
     *
     * Skeletons are exempt: a loading placeholder is deliberately low-contrast, and is not
     * content anyone needs to read.
     */
    @Test
    fun `features do not fade borders or surfaces below usability`() {
        val lowAlpha = Regex("""colorScheme\.(outline|surfaceVariant)\.copy\(alpha = 0\.[0-2]\d*f\)""")
        Konsist.scopeFromProduction()
            .files
            .filter { it.featureModule() != null && !it.name.contains("Skeleton") }
            .assertFalse { file -> file.text.contains(lowAlpha) }
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

    /**
     * No logging where the values are sensitive.
     *
     * On Android `println` goes to logcat, which is readable by anything with log access on a
     * rooted device and is captured verbatim in bug reports. A single debug line in the SMS
     * parser would put message bodies there; one in a repository would put amounts and account
     * ids. This was a real instance, not a hypothetical: an exception in the insights service was
     * being printed, and that message can carry prompt fragments built from the user's spending.
     *
     * Deliberately a build failure rather than a review convention. The cost of logging is
     * invisible at the call site and only shows up in someone else's log capture.
     */
    @Test
    fun `sensitive packages do not log`() {
        Konsist.scopeFromProduction()
            .files
            .filter { file -> sensitivePackages.any { it in ".${file.packagee?.name.orEmpty()}." } }
            .assertFalse { file ->
                file.text.contains("println(") ||
                    file.imports.any { it.name.startsWith("android.util.Log") }
            }
    }
}
