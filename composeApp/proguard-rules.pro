# R8 rules for the release build (isMinifyEnabled = true).
#
# Most libraries here ship their own consumer rules — AGP applies those automatically, so this
# file only covers what R8 cannot infer from the code. Anything kept here is kept because
# something looks it up reflectively at runtime, which R8 cannot see.

# ── kotlinx.serialization ────────────────────────────────────────────────────
# The compiler plugin generates a companion .serializer() per @Serializable class and the
# runtime finds it reflectively. Without this the Gemini request/response DTOs fail to
# serialize in release only — the classic "works in debug, crashes in the store build".
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# ── Ktor ─────────────────────────────────────────────────────────────────────
# Engines are discovered via ServiceLoader, so nothing references them directly.
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# ── Koin ─────────────────────────────────────────────────────────────────────
# Definitions are resolved by type at runtime; keep the modules' own classes intact.
-keep class org.koin.** { *; }
-keep class com.budgetmaster.**.di.** { *; }

# ── SQLDelight / SQLite ──────────────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ── Kotlin coroutines ────────────────────────────────────────────────────────
# The debug agent is absent in release; its absence is expected, not a warning.
-dontwarn kotlinx.coroutines.debug.**

# ── Firebase / GitLive ───────────────────────────────────────────────────────
# GitLive wraps the Firebase Android SDK, which reads model classes reflectively.
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ── Compose ──────────────────────────────────────────────────────────────────
# Keep our own @Serializable navigation routes: Navigation resolves them by type.
-keep class com.budgetmaster.shared.** { *; }

# Line numbers make Crashlytics stack traces readable; the source file name is noise.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
