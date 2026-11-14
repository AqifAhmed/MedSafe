# Default ProGuard rules for MedSafe.

# Room generates implementations reflectively referenced by the runtime.
-keep class com.medsafe.app.data.local.** { *; }

# Keep enum names used by Room type converters.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
