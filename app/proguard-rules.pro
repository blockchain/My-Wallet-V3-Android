-verbose
-dontobfuscate
-ignorewarnings

# These lines allow optimisation whilst preserving stack traces
-optimizations !code/allocation/variable
-optimizations !class/unboxing/enum
-keepattributes SourceFile, LineNumberTable
-keep,allowshrinking,allowoptimization class * { <methods>; }
-keepattributes Signature

# Support V7
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

# Strip all Android logging for security and performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Strip all SLF4J logging in the JAR for security and performance
-assumenosideeffects class * implements org.slf4j.Logger {
    public *** trace(...);
    public *** debug(...);
    public *** info(...);
    public *** warn(...);
    public *** error(...);
}

# Google Play Services
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**

# Don't mess with classes with native methods
-keepclasseswithmembers class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all serializable objects
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# To prevent cases of reflection causing issues
-keepattributes InnerClasses
# Keep custom components in XML
-keep public class custom.components.**

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# To maintain custom components names that are used on layouts XML:
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Specific to Blockchain
-keep class android.support.design.widget.NavigationView { *; }
-keep class piuk.blockchain.android.ui.customviews.AnimatedPasswordInputLayout { *; }

# Retrolambda
-dontwarn java.lang.invoke.*

# bitcoinj
-keep,includedescriptorclasses class org.bitcoinj.wallet.Protos$** { *; }
-keepclassmembers class org.bitcoinj.wallet.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-keep,includedescriptorclasses class org.bitcoin.protocols.payments.Protos$** { *; }
-keepclassmembers class org.bitcoin.protocols.payments.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-dontwarn org.bitcoinj.store.WindowsMMapHack
-dontwarn org.bitcoinj.store.LevelDBBlockStore
-dontnote org.bitcoinj.crypto.DRMWorkaround
-dontnote org.bitcoinj.crypto.TrustStoreLoader$DefaultTrustStoreLoader
-dontnote com.subgraph.orchid.crypto.PRNGFixes
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio
-dontnote com.squareup.okhttp.internal.Platform
-dontwarn org.bitcoinj.store.LevelDBFullPrunedBlockStore**

# zxing
-dontwarn com.google.zxing.common.BitMatrix

# Guava
-dontwarn sun.misc.Unsafe
-dontnote com.google.common.reflect.**
-dontnote com.google.common.util.concurrent.MoreExecutors
-dontnote com.google.common.cache.Striped64,com.google.common.cache.Striped64$Cell

# slf4j
-dontwarn org.slf4j.**

# Apache Commons
-dontwarn org.apache.**

# Retrofit
# Retain service method parameters.
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Jackson
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.** { *; }
-keepclassmembers public final enum com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility {
        public static final com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility *;
}
# Temporary fix: Keep classes for jackson json
-keep public class info.blockchain.api.data.** { *; }
-keep public class info.blockchain.wallet.api.data.** { *; }
-keep public class info.blockchain.wallet.contacts.data.** { *; }
-keep public class info.blockchain.wallet.metadata.data.** { *; }
-keep public class info.blockchain.wallet.payload.data.** { *; }
-keep public class piuk.blockchain.androidbuysell.models.** { *; }
-keep public class piuk.blockchain.androidbuysell.models.coinify.** { *; }
-keep public class piuk.blockchain.android.data.websocket.models.** { *; }
-keep public class info.blockchain.wallet.ethereum.** { *; }
-keep public class info.blockchain.wallet.ethereum.data.** { *; }
-keep public class info.blockchain.wallet.prices.data.** { *; }
-keepclasseswithmembernames class android.support.v4.widget.DrawerLayout { *; }

# Javapoet
-dontwarn com.squareup.javapoet.**

# Spongycastle
-dontwarn org.spongycastle.**
# Bouncycastle
-keep class org.bouncycastle.jce.ECNamedCurveTable

# JJWT
-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }
-dontwarn javax.xml.bind.DatatypeConverter
-dontwarn io.jsonwebtoken.impl.Base64Codec

# Web3j
-dontwarn org.web3j.codegen.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn io.realm.**

# Guava (official)
## Not yet defined: follow https://github.com/google/guava/issues/2117
# Guava (unofficial)
## https://github.com/google/guava/issues/2926#issuecomment-325455128
## https://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
-dontwarn com.google.common.base.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn com.google.errorprone.annotations.ForOverride
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Added for guava 23.5-android
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

# Moshi
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}