# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Hilt
-keep,allowobfuscation,allowshrinking class dagger.hilt.internal.GeneratedComponent
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep,includedescriptorclasses class com.btween.app.**$$serializer { *; }
-keepclassmembers class com.btween.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.btween.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App data models (kept for reflection-based serialization/backup)
-keep class com.btween.app.data.** { *; }
-keep class com.btween.app.domain.model.** { *; }
