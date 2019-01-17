package com.blockchain.network.modules

import com.blockchain.koin.moshiInterceptor
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.TLSSocketFactory
import com.blockchain.serialization.BigDecimalAdaptor
import com.blockchain.serialization.BigIntegerAdapter
import com.squareup.moshi.Moshi
import io.reactivex.schedulers.Schedulers
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val API_TIMEOUT = 30L
private const val PING_INTERVAL = 10L

class OkHttpInterceptors(val list: List<Interceptor>) : List<Interceptor> by list

val apiModule = applicationContext {

    moshiInterceptor("BigDecimal") { builder ->
        builder.add(BigDecimalAdaptor())
    }

    moshiInterceptor("BigInteger") { builder ->
        builder.add(BigIntegerAdapter())
    }

    bean { JacksonConverterFactory.create() }

    bean { RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()) }

    bean {
        CertificatePinner.Builder()
            .add("api.blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.com", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .build()
    }

    bean {
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .certificatePinner(get())

        get<OkHttpInterceptors>().forEach {
            builder.addInterceptor(it)
        }

        /*
          Enable TLS specific version V.1.2
          Issue Details : https://github.com/square/okhttp/issues/1934
         */
        TLSSocketFactory().also {
            builder.sslSocketFactory(it, it.systemDefaultTrustManager())
        }
        builder.build()
    }

    bean("api") {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().apiUrl)
            .client(get())
            .addConverterFactory(get<JacksonConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean("explorer") {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().explorerUrl)
            .client(get())
            .addConverterFactory(get<JacksonConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean {
        Moshi.Builder()
            .also {
                get<MoshiBuilderInterceptorList>()
                    .forEach { interceptor -> interceptor.intercept(it) }
            }
            .build()
    }

    bean {
        MoshiConverterFactory.create(get())
    }

    /**
     * This instance converts to Kotlin data classes ONLY; it will break if used to parse data models
     * written with Java + Jackson.
     */
    bean("kotlin") {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().explorerUrl)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean("kotlin-api") {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().apiUrl)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean("nabu") {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().nabuApi)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }
}
