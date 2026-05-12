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
# Gson uses generic type information stored in a class file when working with fields.
# We must keep Signature, InnerClasses, and EnclosingMethod attributes for TypeToken to work.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Keep Gson's own classes
-keep class com.google.gson.** { *; }

# Keep all data models that are serialized/deserialized
-keep class com.context.data.** { *; }

# Keep anonymous classes that extend TypeToken (crucial for BudgetUtils)
-keep class * extends com.google.gson.reflect.TypeToken

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
