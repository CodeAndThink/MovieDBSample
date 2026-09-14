package com.truongngo.moviedb.data.network

import com.truongngo.moviedb.BuildConfig
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideNetworkInterceptor(): NetworkInterceptor = NetworkInterceptor(
        BuildConfig.MOVIEDB_ACCESS_TOKEN, BuildConfig.MOVIEDB_API_KEY,
    )

    @Provides @Singleton
    fun provideOkHttpClient(interceptor: NetworkInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .connectTimeout(NetworkUtils.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkUtils.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NetworkUtils.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(NetworkUtils.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(NetworkUtils.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideTmdbService(retrofit: Retrofit): TmdbService = retrofit.create(TmdbService::class.java)

    @Provides @Singleton
    fun provideApiClients(implementation: ApiClientsImpl): ApiClients = implementation
}
