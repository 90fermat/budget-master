pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "BudgetMaster"

include(":core")
include(":feature:auth")
include(":feature:dashboard")
include(":feature:transactions")
include(":feature:budgets")
include(":feature:accounts")
include(":feature:reports")
include(":feature:settings")
include(":shared")
include(":composeApp")
include(":webApp")


