# GrokTerm ProGuard / R8 rules

# Keep JavascriptInterface methods for xterm bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# WebView / xterm
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, *);
    public boolean *(android.webkit.WebView, *);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# EncryptedSharedPreferences / Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# DocumentFile
-keep class androidx.documentfile.provider.** { *; }

# Keep LaunchAction / PendingLaunch (simple holder)
-keep class com.finecomputer.grokterm.data.LaunchAction { *; }
-keep class com.finecomputer.grokterm.data.PendingLaunch { *; }

# Optional: strip verbose logs in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
