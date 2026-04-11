package com.nexpos.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
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
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private class FlexibleNumberTypeAdapterFactory : TypeAdapterFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        return when (type.rawType) {
            Int::class.java, java.lang.Integer::class.java -> object : TypeAdapter<Int>() {
                override fun write(out: JsonWriter, value: Int?) {
                    if (value == null) out.nullValue() else out.value(value)
                }
                override fun read(input: JsonReader): Int {
                    return when (input.peek()) {
                        JsonToken.STRING -> input.nextString().toIntOrNull() ?: 0
                        JsonToken.NUMBER -> input.nextInt()
                        JsonToken.NULL -> { input.nextNull(); 0 }
                        else -> { input.skipValue(); 0 }
                    }
                }
            } as TypeAdapter<T>

            Long::class.java, java.lang.Long::class.java -> object : TypeAdapter<Long>() {
                override fun write(out: JsonWriter, value: Long?) {
                    if (value == null) out.nullValue() else out.value(value)
                }
                override fun read(input: JsonReader): Long {
                    return when (input.peek()) {
                        JsonToken.STRING -> input.nextString().toLongOrNull() ?: 0L
                        JsonToken.NUMBER -> input.nextLong()
                        JsonToken.NULL -> { input.nextNull(); 0L }
                        else -> { input.skipValue(); 0L }
                    }
                }
            } as TypeAdapter<T>

            else -> null
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
            .registerTypeAdapterFactory(FlexibleNumberTypeAdapterFactory())
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
