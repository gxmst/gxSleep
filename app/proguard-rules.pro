# Keep Room entities
-keep class com.gx.sleep.data.local.entity.** { *; }
-keep class com.gx.sleep.data.local.database.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
