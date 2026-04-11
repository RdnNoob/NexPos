# ============= Kotlin =============
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ============= Coroutines =============
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============= Retrofit =============
# Atribut wajib agar generic type (Response<AuthResponse>) tidak dihapus R8
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-keep class retrofit2.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# KRITIS: Pertahankan interface NexPosApi agar Retrofit bisa baca
# generic return type (Response<AuthResponse>) saat runtime
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# Pertahankan parameter metode suspend function (Kotlin coroutines)
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============= OkHttp =============
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============= Gson =============
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============= Data Models =============
-keep class com.nexpos.core.data.model.** { *; }
-keepclassmembers class com.nexpos.core.data.model.** { *; }

# ============= DataStore =============
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============= Hilt / Dagger =============
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Pertahankan Application @HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Pertahankan Activity/Fragment @AndroidEntryPoint
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Pertahankan ViewModel @HiltViewModel beserta constructor-nya
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Pertahankan kelas-kelas generated Hilt
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_ComponentTreeDeps { *; }
-keep class **_MembersInjector { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep class hilt_aggregated_deps.** { *; }

# Pertahankan semua class @Inject constructor
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}

# ============= Compose =============
-dontwarn androidx.compose.**
