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

-keep class com.onfido.android.sdk.** {
  public protected private *;
}
-keep class com.onfido.api.client.** {
  public protected private *;
}

# This isn't necessary as of AGP 3.2-beta01
# https://issuetracker.google.com/issues/79874119
# TODO: Remove this once upgraded to 3.2, which is necessary for the KYC release AND-1237
-keep class com.blockchain.kycui.** extends android.support.v4.app.Fragment{}
-keep public class com.blockchain.kyc.models.** { *; }