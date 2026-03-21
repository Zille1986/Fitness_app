# Add project specific ProGuard rules here.

# ── Data models (Room entities, serialized classes) ──────────────
-keep class com.runtracker.shared.data.model.** { *; }
-keep class com.runtracker.app.health.** { *; }

# ── Gson ─────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── Retrofit ─────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Room ─────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }

# ── Compose ──────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ── Firebase ─────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Health Connect ───────────────────────────────────────────────
-keep class androidx.health.connect.** { *; }
-keep class androidx.health.services.** { *; }

# ── ML Kit (Pose Detection) ─────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── Coil (Image loading) ────────────────────────────────────────
-dontwarn coil.**

# ── DataStore ────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── Kotlin Serialization / Coroutines ────────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# ── General ──────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable  # Better crash reports
-renamesourcefileattribute SourceFile
