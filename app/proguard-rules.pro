# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep osmdroid classes
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep signal logging classes
-keep class com.signaldrivelogger.** { *; }

# Keep data classes
-keep class * implements java.io.Serializable { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
