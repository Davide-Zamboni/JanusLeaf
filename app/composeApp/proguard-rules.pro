# Keep Ktor classes
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep app models
-keep class com.janusleaf.app.domain.model.** { *; }
-keep class com.janusleaf.app.data.remote.dto.** { *; }

# Keep Koin
-keep class org.koin.** { *; }
