# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-dontwarn okhttp3.internal.platform.ConscryptPlatform
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keepattributes Signature
-keepattributes Exceptions
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn sun.misc.**


#kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.geckour.nowplaying4gpm.**$$serializer { *; }
-keepclassmembers class com.geckour.nowplaying4gpm.** {
    *** Companion;
}
-keepclasseswithmembers class com.geckour.nowplaying4gpm.** {
    kotlinx.serialization.KSerializer serializer(...);
}


#CardView
-dontwarn android.databinding.adapters.CardViewBindingAdapter


#Twitter4J
-keep class twitter4j.** { *; }
-dontwarn javax.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.ManagementFactory


# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**


# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}