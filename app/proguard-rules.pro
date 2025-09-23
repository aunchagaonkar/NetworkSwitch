# Network Switch ProGuard Rules - Optimized for size

# Aggressive optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 10
-allowaccessmodification
-dontpreverify
-overloadaggressively
-repackageclasses ''

# Remove debug logging completely in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep AIDL interfaces
-keep class com.supernova.networkswitch.IRootController { *; }
-keep class com.supernova.networkswitch.IShizukuController { *; }

# Keep classes that interact with system APIs
-keep class com.supernova.networkswitch.service.** { *; }
-keep class com.supernova.networkswitch.data.source.** { *; }

# Keep reflection classes used for network mode constants
-keepclassmembers class com.supernova.networkswitch.data.source.** {
    public static final int *;
    public static final long *;
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.modules.ApplicationContextModule
-keep class **_HiltModules$*
-keep class **_MembersInjector

# Keep libsu classes
-keep class com.topjohnwu.superuser.** { *; }

# Keep Shizuku classes
-keep class rikka.shizuku.** { *; }

# Keep Compose classes - more selective
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }
# Remove overly broad Compose keep rule
# -keep class androidx.compose.** { *; }

# Keep Android system classes that might be accessed via reflection
-keep class android.telephony.** { *; }
-keep class com.android.internal.telephony.** { *; }

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Minimal attributes for crash reports (removed SourceFile for smaller APK)
-keepattributes LineNumberTable
# -keepattributes SourceFile  # Commented out for smaller APK
# -renamesourcefileattribute SourceFile  # Commented out

# Additional optimizations
-repackageclasses ''
-allowaccessmodification