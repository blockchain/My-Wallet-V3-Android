@file:Suppress("unused")

object Versions {

    // Release info
    const val minSdk = 17
    const val targetSdk = 27
    const val compileSdk = 27
    const val versionCode = 311
    const val versionName = "6.12.0-rc5"
    const val buildTools = "27.0.3"

    // Build tools and languages
    const val androidPlugin = "3.1.3"
    const val kotlin = "1.2.51"
    const val googleServicesPlugin = "4.0.1"
    const val coveralls = "2.8.2"
    const val buildProperties = "0.4"
    const val ktlint = "0.27.0"
    const val kotlinJvmTarget = "1.6"
    const val javaCompatibilityVersion = 1.7

    // Support Libraries
    const val supportLibs = "27.1.1"
    const val googleServices = "15.0.1"
    const val firebaseMessaging = "17.0.0"
    const val firebaseCore = "16.0.1"
    const val constraintLayout = "1.0.2"
    const val supportTesting = "1.0.2"
    const val multidex = "1.0.2"
    const val navigation = "1.0.0-alpha05"
    const val lifecycle_version = "1.1.1"

    // Networking, RxJava
    const val retrofit = "2.4.0"
    const val okHttp = "3.10.0"
    const val mockWebServer = "v0.1.0"
    const val okIo = "1.14.1"
    const val moshi = "1.6.0"
    const val jacksonCore = "2.9.5"
    const val dagger = "2.16"
    const val koin = "0.9.3"
    const val rxJava = "2.1.17"
    const val rxKotlin = "2.2.0"
    const val rxAndroid = "2.0.2"
    const val rxBinding = "2.1.1"
    const val rxReplayShare = "2.0.1"
    const val rxFingerprint = "2.2.1"

    // Utils, Ethereum
    const val web3j = "3.3.1-android"
    const val spongycastle = "1.53.0.0"
    const val jjwt = "0.9.0"
    const val lambdaWorks = "1.0.0"
    const val libPhoneNumber = "8.9.10"
    // Keep at 1.3 to match Android
    const val commonsCodec = "1.3"
    const val commonsLang = "3.4"
    const val commonsCli = "1.3"
    const val commonsIo = "2.4"
    const val urlBuilder = "2.0.8"
    const val yearclass = "2.0.0"
    const val protobuf = "2.6.1"
    const val findbugs = "2.0.1"
    const val guava = "24.0-android"
    const val dexter = "4.2.0"

    // Custom Views
    const val charts = "3.0.3"
    const val circleIndicator = "1.2.2"
    const val bottomNav = "2.2.0"
    const val countryPicker = "1.1.7"
    const val zxing = "3.3.0"
    const val wheelPicker = "1.1.2"
    const val konfetti = "1.1.1"

    // Third Party SDKs
    const val onfido = "4.3.0"

    // Logging
    const val timber = "4.6.0"
    const val slf4j = "1.7.20"
    const val crashlytics = "2.9.4"
    const val fabricTools = "1.24.4"

    // Testing
    const val mockito = "2.10.0"
    const val mockitoKotlin = "1.5.0"
    const val kluent = "1.19"
    const val hamcrestJunit = "2.0.0.0"
    const val junit = "4.12"
    const val robolectric = "3.8"
    const val json = "20140107"
    const val espresso = "3.0.1"
    const val jacoco = "0.8.1"
}

object Libraries {

    // Build tools and languages
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidPlugin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val coveralls = "org.kt3k.gradle.plugin:coveralls-gradle-plugin:${Versions.coveralls}"
    const val googleServicesPlugin =
        "com.google.gms:google-services:${Versions.googleServicesPlugin}"
    const val buildProperties =
        "com.novoda:gradle-build-properties-plugin:${Versions.buildProperties}"
    const val ktlint = "com.github.shyiko:ktlint:${Versions.ktlint}"

    // Support Libraries
    const val appCompat = "com.android.support:appcompat-v7:${Versions.supportLibs}"
    const val recyclerView = "com.android.support:recyclerview-v7:${Versions.supportLibs}"
    const val cardView = "com.android.support:cardview-v7:${Versions.supportLibs}"
    const val gridLayout = "com.android.support:gridlayout-v7:${Versions.supportLibs}"
    const val design = "com.android.support:design:${Versions.supportLibs}"
    const val v13 = "com.android.support:support-v13:${Versions.supportLibs}"
    const val v14 = "com.android.support:preference-v14:${Versions.supportLibs}"
    const val dynamicAnims = "com.android.support:support-dynamic-animation:${Versions.supportLibs}"
    const val annotations = "com.android.support:support-annotations:${Versions.supportLibs}"
    const val constraintLayout =
        "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"
    const val dataBindingKapt = "com.android.databinding:compiler:${Versions.androidPlugin}"
    const val multidex = "com.android.support:multidex:${Versions.multidex}"
    const val navigationControllerCore = "android.arch.navigation:navigation-ui:${Versions.navigation}"
    const val navigationControllerFragments = "android.arch.navigation:navigation-fragment:${Versions.navigation}"
    const val lifecycleViewModel = "android.arch.lifecycle:viewmodel:${Versions.lifecycle_version}"
    const val lifecycleExtensions = "android.arch.lifecycle:extensions:${Versions.lifecycle_version}"

    // Google & Firebase
    const val firebaseCore = "com.google.firebase:firebase-core:${Versions.firebaseCore}"
    const val firebaseMessaging =
        "com.google.firebase:firebase-messaging:${Versions.firebaseMessaging}"
    const val googlePlayServicesBase =
        "com.google.android.gms:play-services-base:${Versions.googleServices}"
    const val googlePlaces = "com.google.android.gms:play-services-places:${Versions.googleServices}"

    // Networking, RxJava
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitJacksonConverter =
        "com.squareup.retrofit2:converter-jackson:${Versions.retrofit}"
    const val retrofitRxMoshiConverter =
        "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"
    const val retrofitRxJavaAdapter = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    const val okHttpInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"
    const val okIo = "com.squareup.okio:okio:${Versions.okIo}"
    const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"
    const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${Versions.moshi}"
    const val jacksonCore = "com.fasterxml.jackson.core:jackson-core:${Versions.jacksonCore}"
    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerKapt = "com.google.dagger:dagger-compiler:${Versions.dagger}"
    const val koin = "org.koin:koin-core:${Versions.koin}"
    const val koinAndroid = "org.koin:koin-android:${Versions.koin}"
    const val koinAndroidArchitecture = "org.koin:koin-android-architecture:${Versions.koin}"
    const val koinTest = "org.koin:koin-test:${Versions.koin}"
    const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
    const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
    const val rxBinding = "com.jakewharton.rxbinding2:rxbinding-kotlin:${Versions.rxBinding}"
    const val rxBindingV4 = "com.jakewharton.rxbinding2:rxbinding-support-v4-kotlin:${Versions.rxBinding}"
    const val rxBindingV7 = "com.jakewharton.rxbinding2:rxbinding-appcompat-v7-kotlin:${Versions.rxBinding}"
    const val rxReplayShare = "com.jakewharton.rx2:replaying-share-kotlin:${Versions.rxReplayShare}"
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
    const val protobuf = "com.google.protobuf:protobuf-java:${Versions.protobuf}"
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

    // Third Party SDKs
    const val onfido = "com.onfido.sdk.capture:onfido-capture-sdk:${Versions.onfido}"

    // Logging
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    const val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"
    const val slf4jNoOp = "org.slf4j:slf4j-nop:${Versions.slf4j}"
    const val crashlytics = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}@aar"
    const val fabricTools = "io.fabric.tools:gradle:${Versions.fabricTools}"

    // Testing
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val hamcrestJunit = "org.hamcrest:hamcrest-junit:${Versions.hamcrestJunit}"
    const val junit = "junit:junit:${Versions.junit}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val json = "org.json:json:${Versions.json}"
    const val testRules = "com.android.support.test:rules:${Versions.supportTesting}"
    const val testRunner = "com.android.support.test:runner:${Versions.supportTesting}"
    const val espresso = "com.android.support.test.espresso:espresso-core:${Versions.espresso}"
    const val retrofitMock = "com.squareup.retrofit2:retrofit-mock:${Versions.retrofit}"
    const val okHttpMock = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
    const val mockWebServer = "com.github.fabric8io:mockwebserver:${Versions.mockWebServer}"
    const val jacoco = "org.jacoco:org.jacoco.core:${Versions.jacoco}"
}