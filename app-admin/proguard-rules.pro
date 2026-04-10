-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nexpos.core.data.model.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
