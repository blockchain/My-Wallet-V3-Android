@file:Suppress("unused")

object Versions {

    // Release info
    const val minSdk = 21
    const val targetSdk = 29
    const val compileSdk = 28

    const val versionCode = 560
    const val versionName = "6.36.2"

    const val buildTools = "28.0.3"

    // Build tools and languages
    const val androidPlugin = "3.4.1"
    const val kotlin = "1.3.30"
    const val googleServicesPlugin = "4.3.3"
    const val coveralls = "2.8.2"
    const val buildProperties = "0.4"
    const val ktlint = "0.27.0"
    const val kotlinJvmTarget = "1.6"
    const val javaCompatibilityVersion = 1.7

    // Androidx Libraries
    const val appCompact = "1.1.0"
    const val recyclerview = "1.1.0"
    const val cardview = "1.0.0"
    const val gridlayout = "1.0.0"
    const val design = "1.1.0"
    const val preference = "1.1.0"
    const val dynamicanimation = "1.0.0"
    const val annotations = "1.1.0"
    const val constraintLayout = "1.1.0"
    const val multidex = "2.0.1"
    const val navigation = "2.1.0"
    const val lifecycle = "2.1.0"

    // Support Libraries
    const val googleServices = "17.1.0"
    const val googleServicesPlaces = "17.0.0"
    const val googlePlayCore = "1.6.1"
    const val firebaseMessaging = "20.2.0"
    const val firebaseCore = "17.4.2"
    const val firebaseConfig = "19.1.0"
    const val firebaseDynamicLink = "19.0.0"
    const val supportTesting = "1.0.2"

    // Networking, RxJava
    const val retrofit = "2.7.0"
    const val okHttp = "4.0.1"
    const val mockWebServer = "v0.1.8"
    const val moshi = "1.8.0"
    const val gson = "2.8.5"
    const val jacksonCore = "2.10.0"
    const val koin = "2.1.5"
    const val rxJava = "2.2.11"
    const val rxKotlin = "2.4.0"
    const val rxAndroid = "2.1.1"
    const val rxBinding = "2.1.1"
    const val rxReplayShare = "2.0.1"
    const val rxFingerprint = "2.2.1"
    const val rxRelay = "2.1.1"

    // Utils, Ethereum
    const val web3j = "3.3.1-android"
    const val spongycastle = "1.54.0.0"
    const val jjwt = "0.9.0"
    const val lambdaWorks = "1.0.0"
    const val libPhoneNumber = "8.9.10"
    const val commonsCodec = "1.3" // Keep at 1.3 to match Android
    const val commonsLang = "3.4"
    const val commonsCli = "1.3"
    const val commonsIo = "2.6"
    const val urlBuilder = "2.0.9"
    const val yearclass = "2.0.0"
    const val protobuf = "3.0.1"
    const val findbugs = "3.0.2"
    const val guava = "28.0-android"
    const val dexter = "5.0.0"

    // Custom Views
    const val charts = "3.1.0"
    const val circleIndicator = "1.2.2"
    const val bottomNav = "2.2.0"
    const val countryPicker = "1.1.7"

    // zxing 3.4.0 crashes with:
    //      "java.lang.NoSuchMethodError. No interface method sort(Ljava/util/Comparator;)V in class Ljava/util/List;"
    // List.sort() is not available on Android SDK < 24 so DO NOT UPGRADE until project target min is 24
    const val zxing = "3.3.0"
    const val wheelPicker = "1.1.2"
    const val konfetti = "1.1.3"
    const val materialDatePicker = "3.6.4"
    const val sparkline = "1.2.0"

    // Third Party SDKs
    const val veriff = "3.1.0"
    const val probity = "1.0.0"
    const val cardForm = "4.2.0"

    // Logging
    const val timber = "4.7.1"
    const val slf4j = "1.7.20"
    const val firebaseCrashlytics = "17.0.0"
    const val firebaseCrashlyticsPlugin = "2.1.0"
    const val firebaseAnalytics = "17.4.2"

    // Debugging
    const val stetho = "1.5.1"

    // Testing
    const val mockito = "2.10.0"
    const val mockitoKotlin = "1.5.0"
    const val kluent = "1.19"
    const val hamcrestJunit = "2.0.0.0"
    const val junit = "4.12"
    const val robolectric = "4.3"
    const val json = "20140107"
    const val espresso = "3.2.0"
    const val jacoco = "0.8.1"
    const val androidxTesting = "1.1.1"
}

object Libraries {

    // Build tools and languages
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidPlugin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val coveralls = "org.kt3k.gradle.plugin:coveralls-gradle-plugin:${Versions.coveralls}"
    const val googleServicesPlugin =
        "com.google.gms:google-services:${Versions.googleServicesPlugin}"
    const val buildProperties =
        "com.novoda:gradle-build-properties-plugin:${Versions.buildProperties}"
    const val ktlint = "com.github.shyiko:ktlint:${Versions.ktlint}"

    // Support Libraries
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompact}"
    const val recyclerView = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"
    const val cardView = "androidx.cardview:cardview:${Versions.cardview}"
    const val gridLayout = "androidx.gridlayout:gridlayout:${Versions.gridlayout}"
    const val design = "com.google.android.material:material:${Versions.design}"
    const val v14 = "androidx.preference:preference:${Versions.preference}"
    const val dynamicAnims =
        "androidx.dynamicanimation:dynamicanimation:${Versions.dynamicanimation}"
    const val annotations = "androidx.annotations:annotations:${Versions.annotations}"
    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val multidex = "androidx.multidex:multidex:${Versions.multidex}"

    const val navigationControllerCore = "androidx.navigation:navigation-ui:${Versions.navigation}"
    const val navigationControllerFragments =
        "androidx.navigation:navigation-fragment:${Versions.navigation}"
    const val navigationControllerSafeArgsPlugin =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}"
    const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"

    // Google & Firebase
    const val firebaseCore = "com.google.firebase:firebase-core:${Versions.firebaseCore}"
    const val firebaseConfig = "com.google.firebase:firebase-config:${Versions.firebaseConfig}"
    const val firebaseMessaging =
        "com.google.firebase:firebase-messaging:${Versions.firebaseMessaging}"
    const val firebaseDynamicLink =
        "com.google.firebase:firebase-dynamic-links:${Versions.firebaseDynamicLink}"
    const val googlePlayServicesBase =
        "com.google.android.gms:play-services-base:${Versions.googleServices}"
    const val googlePlaces =
        "com.google.android.gms:play-services-places:${Versions.googleServicesPlaces}"
    const val googlePlayCore = "com.google.android.play:core:${Versions.googlePlayCore}"

    // Networking, RxJava
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitJacksonConverter =
        "com.squareup.retrofit2:converter-jackson:${Versions.retrofit}"
    const val retrofitRxMoshiConverter =
        "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"
    const val retrofitRxJavaAdapter = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    const val retrofitGsonConverter = "com.squareup.retrofit2:converter-gson:2.5.0"
    const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    const val okHttpSse = "com.squareup.okhttp3:okhttp-sse:${Versions.okHttp}"
    const val okHttpInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"
    const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"
    const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${Versions.moshi}"
    const val moshiAdapters = "com.squareup.moshi:moshi-adapters:${Versions.moshi}"
    const val jacksonCore = "com.fasterxml.jackson.core:jackson-core:${Versions.jacksonCore}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"
    const val koin = "org.koin:koin-core:${Versions.koin}"
    const val koinAndroid = "org.koin:koin-android:${Versions.koin}"

    const val koinTest = "org.koin:koin-test:${Versions.koin}"
    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
    const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
    const val rxBinding = "com.jakewharton.rxbinding2:rxbinding-kotlin:${Versions.rxBinding}"
    const val rxBindingV4 =
        "com.jakewharton.rxbinding2:rxbinding-support-v4-kotlin:${Versions.rxBinding}"
    const val rxBindingV7 =
        "com.jakewharton.rxbinding2:rxbinding-appcompat-v7-kotlin:${Versions.rxBinding}"
    const val rxReplayShare = "com.jakewharton.rx2:replaying-share-kotlin:${Versions.rxReplayShare}"
    const val rxRelay = "com.jakewharton.rxrelay2:rxrelay:${Versions.rxRelay}"
    const val rxFingerprint = "com.mtramin:rxfingerprint:${Versions.rxFingerprint}"

    // Utils, Ethereum
    const val web3j = "org.web3j:core:${Versions.web3j}"
    const val spongyCastle = "com.madgag.spongycastle:prov:${Versions.spongycastle}"
    const val jjwt = "io.jsonwebtoken:jjwt:${Versions.jjwt}"
    const val lambdaWorks = "com.lambdaworks:codec:${Versions.lambdaWorks}"
    const val commonsCodec = "commons-codec:commons-codec:${Versions.commonsCodec}"
    const val commonsLang = "org.apache.commons:commons-lang3:${Versions.commonsLang}"
    const val commonsCli = "commons-cli:commons-cli:${Versions.commonsCli}"
    const val commonsIo = "commons-io:commons-io:${Versions.commonsIo}"
    const val urlBuilder = "io.mikael:urlbuilder:${Versions.urlBuilder}"
    const val yearclass = "com.facebook.device.yearclass:yearclass:${Versions.yearclass}"
    const val protobuf = "com.google.protobuf:protobuf-lite:${Versions.protobuf}"
    const val findbugs = "com.google.code.findbugs:jsr305:${Versions.findbugs}"
    const val guava = "com.google.guava:guava:${Versions.guava}"
    const val dexter = "com.karumi:dexter:${Versions.dexter}"
    const val libPhoneNumber = "io.michaelrocks:libphonenumber-android:${Versions.libPhoneNumber}"

    // Custom Views
    const val charts = "com.github.PhilJay:MPAndroidChart:v${Versions.charts}"
    const val circleIndicator = "me.relex:circleindicator:${Versions.circleIndicator}@aar"
    const val bottomNav = "com.aurelhubert:ahbottomnavigation:${Versions.bottomNav}"
    const val countryPicker =
        "com.github.mukeshsolanki:country-picker-android:${Versions.countryPicker}"
    const val zxing = "com.google.zxing:core:${Versions.zxing}"
    const val wheelPicker = "cn.aigestudio.wheelpicker:WheelPicker:${Versions.wheelPicker}"
    const val konfetti = "nl.dionsegijn:konfetti:${Versions.konfetti}"
    const val materialDatePicker =
        "com.wdullaer:materialdatetimepicker:${Versions.materialDatePicker}"
    const val sparkline = "com.robinhood.spark:spark:${Versions.sparkline}"

    // Third Party SDKs
    const val veriff = "com.veriff:veriff-library:${Versions.veriff}"
    const val probity = "io.probity.sdk:collector:${Versions.probity}"
    const val cardForm = "com.braintreepayments:card-form:${Versions.cardForm}"

    // Logging
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    const val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"
    const val slf4jNoOp = "org.slf4j:slf4j-nop:${Versions.slf4j}"
    const val firebaseCrashlytics =
        "com.google.firebase:firebase-crashlytics:${Versions.firebaseCrashlytics}"
    const val firebaseCrashlyticsPlugin =
        "com.google.firebase:firebase-crashlytics-gradle:${Versions.firebaseCrashlyticsPlugin}"
    const val firebaseAnalytics = "com.google.firebase:firebase-analytics:${Versions.firebaseAnalytics}"

    // Debugging
    const val stetho = "com.facebook.stetho:stetho:${Versions.stetho}"
    const val stethoOkHttp = "com.facebook.stetho:stetho-okhttp3:${Versions.stetho}"

    // Testing
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val hamcrestJunit = "org.hamcrest:hamcrest-junit:${Versions.hamcrestJunit}"
    const val junit = "junit:junit:${Versions.junit}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val json = "org.json:json:${Versions.json}"
    const val testRules = "androidx.test:rules:${Versions.androidxTesting}"
    const val testRunner = "androidx.test:runner:${Versions.androidxTesting}"
    const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    const val retrofitMock = "com.squareup.retrofit2:retrofit-mock:${Versions.retrofit}"
    const val okHttpMock = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
    const val mockWebServer = "com.github.fabric8io:mockwebserver:${Versions.mockWebServer}"
    const val jacoco = "org.jacoco:org.jacoco.core:${Versions.jacoco}"
}
