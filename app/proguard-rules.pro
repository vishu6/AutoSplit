# Jetpack Glance / App Widget protection
-keep class androidx.glance.appwidget.** { *; }
-keep class com.context.app.widget.** { *; }

# Firebase Realtime Database protection
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.context.sync.** { *; }
-keepclassmembers class com.context.sync.** {
  public <init>(...);
}

# Ensure MainActivity and its extras are not mangled for Widget Trampoline
-keep class com.context.app.MainActivity { *; }

# Compose / Material 3
-keep class androidx.compose.material3.** { *; }
