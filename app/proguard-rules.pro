# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Room Database Protection ---
# Room uses reflection to access your entity classes.
# We must keep the classes and their members (fields).
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }

# --- Gson Protection ---
# If you use Gson to serialize/deserialize classes, keep them.
# Adjust the package name to match your data models.
-keep class com.context.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# --- Firebase & Crashlytics ---
-keepattributes SourceFile,LineNumberTable
-keep public class * extends com.google.firebase.messaging.FirebaseMessagingService

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class com.google.dagger.** { *; }

# --- General Polish ---
-dontwarn okio.**
-dontwarn javax.annotation.**
