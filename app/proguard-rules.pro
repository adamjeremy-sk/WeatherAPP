# Keep generic signatures and runtime annotations required by Gson
-keepattributes Signature
-keepattributes *Annotation*

# Gson TypeToken and its anonymous subclasses (List/Map type capture)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Gson UnsafeAllocator references sun.misc.Unsafe, absent on Android
-dontwarn sun.misc.**

# Keep KXml and XmlPull parser used in CAP-CP alert parsing
-keep class org.xmlpull.v1.** { *; }
-keep class org.kxml2.io.KXmlParser { *; }

# Prevent R8 build aborts on OkHttp/Okio platform reflection warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# WorkManager internals
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Strip debug and verbose logs in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
