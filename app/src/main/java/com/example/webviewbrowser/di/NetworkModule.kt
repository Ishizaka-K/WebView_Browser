package com.example.webviewbrowser.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * ネットワーク関連の DI。OkHttp の単一インスタンスを提供する。
 * ダウンロードエンジン(Task6)などが利用する。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // ダウンロードは進捗制御のため自動リトライを無効化する。
            .retryOnConnectionFailure(false)
            .build()
}
