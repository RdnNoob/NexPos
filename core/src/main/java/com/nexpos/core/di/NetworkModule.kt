package com.nexpos.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
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
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            // Int field menerima JSON string → coba parseInt, fallback 0
            .registerTypeAdapter(Int::class.java, object : JsonDeserializer<Int> {
                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type,
                    context: JsonDeserializationContext
                ): Int {
                    return try {
                        if (json.isJsonPrimitive) {
                            val p = json.asJsonPrimitive
                            when {
                                p.isNumber -> p.asInt
                                p.isString -> p.asString.trim().toIntOrNull() ?: 0
                                else -> 0
                            }
                        } else 0
                    } catch (e: Exception) { 0 }
                }
            })
            // String field menerima JSON number → konversi ke string
            .registerTypeAdapter(String::class.java, object : JsonDeserializer<String> {
                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type,
                    context: JsonDeserializationContext
                ): String {
                    return try {
                        if (json.isJsonPrimitive) {
                            val p = json.asJsonPrimitive
                            when {
                                p.isString -> p.asString
                                p.isNumber -> p.asNumber.toString()
                                p.isBoolean -> p.asBoolean.toString()
                                else -> p.asString
                            }
                        } else json.toString()
                    } catch (e: Exception) { json.toString() }
                }
            })
            .create()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit {
        val baseUrl = BuildConfig.BASE_URL.let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
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
