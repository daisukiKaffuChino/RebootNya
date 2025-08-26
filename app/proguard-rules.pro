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
-keepattributes Signature
-keepattributes *Annotation*

-keep class * extends android.os.IInterface { *; }
-keep interface * extends android.os.IInterface { *; }

# 保持 ShizukuUtil 类及其方法不被混淆
-keep class github.daisukikaffuchino.rebootnya.utils.ShizukuUtil {
    private int shizukuProcess(java.lang.String[]);
}

# 保持 Shizuku 类的 service 字段不被混淆
-keep class rikka.shizuku.Shizuku {
    private static moe.shizuku.server.IShizukuService service;
}

# 保持 IShizukuService 接口不被混淆
-keep interface moe.shizuku.server.IShizukuService { *; }

# 保持 NyaRemoteProcess 类不被混淆
-keep class github.daisukikaffuchino.rebootnya.shizuku.NyaRemoteProcess { *; }

-keep class com.topjohnwu.superuser.** { *; }
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service