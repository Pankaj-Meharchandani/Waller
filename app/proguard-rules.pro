# Waller R8 / Proguard Rules

# ── SLF4J (Fixes Ktor build failure) ──────────────────────────────────────
-dontwarn org.slf4j.**

# ── Ktor & OkHttp ────────────────────────────────────────────────────────
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# ── Serialization ─────────────────────────────────────────────────────────
# Keep your data models so they don't get renamed (breaking JSON parsing)
-keep class com.example.waller.ui.wallfile.** { *; }
-keep class com.example.waller.ui.wallpaper.** { *; }
-keep class com.example.waller.data.network.** { *; }

# General Kotlinx Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class **$serializer {
    kotlinx.serialization.KSerializer INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}
