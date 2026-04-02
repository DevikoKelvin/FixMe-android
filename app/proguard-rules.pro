# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve GSON related classes
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Preserve your model classes in the objects package
-keep class com.erela.fixme.objects.** { *; }

# Keep SerializedName fields
-keepclasseswithmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Renderscript
-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }
