package com.nexpos.core.di

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.nexpos.core.BuildConfig
import com.nexpos.core.data.api.NexPosApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private object FlexibleIntDeserializer : JsonDeserializer<Int> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Int {
        return try {
            json.asInt
        } catch (_: Exception) {
            json.asString.toIntOrNull() ?: 0
        }
    }
}

private object FlexibleLongDeserializer : JsonDeserializer<Long> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Long {
        return try {
            json.asLong
        } catch (_: Exception) {
            json.asString.toLongOrNull() ?: 0L
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val baseUrl = BuildConfig.BASE_URL.let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
        val gson = GsonBuilder()
            .setLenient()
            .registerTypeAdapter(Int::class.java, FlexibleIntDeserializer)
            .registerTypeAdapter(Int::class.javaObjectType, FlexibleIntDeserializer)
            .registerTypeAdapter(Long::class.java, FlexibleLongDeserializer)
            .registerTypeAdapter(Long::class.javaObjectType, FlexibleLongDeserializer)
            .create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideNexPosApi(retrofit: Retrofit): NexPosApi {
        return retrofit.create(NexPosApi::class.java)
    }
}
