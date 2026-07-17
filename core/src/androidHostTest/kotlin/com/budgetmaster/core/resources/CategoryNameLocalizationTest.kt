package com.budgetmaster.core.resources

import com.budgetmaster.core.db.DefaultData
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `DefaultData` seeds category names into the database in English, so a stored name is only ever
 * translated for categories the app itself created. The UI resolves seeded ids through
 * `category_*` string resources instead — but only for ids that actually have one: anything
 * missing silently falls back to the English name in the database, which reads as a bug only to
 * someone running the app in French.
 *
 * Adding a seed category without its strings is the easy way to reintroduce that, so this fails
 * the build instead.
 */
class CategoryNameLocalizationTest {

    private val localeDirs = listOf(
        "src/commonMain/composeResources/values",
        "src/commonMain/composeResources/values-fr",
    )

    /** The `name` of every `<string>` declared in the given locale's strings.xml. */
    private fun declaredKeys(dir: String): Set<String> =
        Regex("""<string name="([^"]+)"""")
            .findAll(File(dir, "strings.xml").readText())
            .map { it.groupValues[1] }
            .toSet()

    /** "cat_food" -> "category_food", matching the mapping in `categoryNameFor`. */
    private fun resourceKeyFor(categoryId: String) = categoryId.replace("cat_", "category_")

    @Test
    fun seededCategoriesExist() {
        // Guards the guard: an empty seed list would make the assertions below vacuously pass.
        assertTrue(DefaultData.categories.isNotEmpty(), "Expected seeded categories")
    }

    @Test
    fun everySeededCategoryHasALocalizedNameInEveryLocale() {
        localeDirs.forEach { dir ->
            val keys = declaredKeys(dir)
            val missing = DefaultData.categories
                .map { resourceKeyFor(it.id) }
                .filterNot { it in keys }

            assertTrue(
                missing.isEmpty(),
                "$dir/strings.xml is missing a display name for seeded categories: $missing. " +
                    "Without it the UI falls back to the English name seeded into the database.",
            )
        }
    }
}
