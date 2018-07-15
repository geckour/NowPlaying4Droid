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

-dontwarn android.databinding.adapters.CardViewBindingAdapter

-dontwarn okhttp3.internal.platform.ConscryptPlatform

-keepattributes Signature

-dontwarn sun.misc.**

-keep class com.google.gson.examples.android.model.** { *; }

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepnames class com.geckour.nowplaying4gpm.api.model.** { *; }
-keepnames class com.geckour.nowplaying4gpm.util.WidgetState { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class * implements com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class twitter4j.** { *; }
-dontwarn javax.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.ManagementFactory
