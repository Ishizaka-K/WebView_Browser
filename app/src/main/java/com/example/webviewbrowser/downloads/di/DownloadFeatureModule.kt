package com.example.webviewbrowser.downloads.di

import com.example.webviewbrowser.downloads.engine.DefaultDownloadEngine
import com.example.webviewbrowser.downloads.engine.DownloadEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** downloads feature の DI（本タスク所有）。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadFeatureModule {

    @Binds
    @Singleton
    abstract fun bindDownloadEngine(impl: DefaultDownloadEngine): DownloadEngine
}
