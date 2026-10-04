# --- Compose ---
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# --- Retrofit ---
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class com.haoze.keynote.data.remote.** { *; }

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Gson ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# --- DataStore ---
-keepclassmembers class * extends androidx.datastore.preferences.** { *; }

# --- Navigation ---
-keep class * extends androidx.navigation.NavType { *; }

# --- compose-markdown ---
-keep class com.github.jeziellago.composemarkdown.** { *; }
