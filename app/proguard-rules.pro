-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# Keep domain models for reflection or state restoration
-keep class com.chaudharyjatin115.pixelia.domain.model.** { *; }

# Allow R8 full optimization on Coil & Media3 while suppressing non-critical library warnings
-dontwarn coil3.**
-dontwarn androidx.media3.**
-dontwarn dev.chrisbanes.haze.**

# Strip verbose/debug logs in release builds to eliminate string allocations, CPU overhead and data leakage
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
