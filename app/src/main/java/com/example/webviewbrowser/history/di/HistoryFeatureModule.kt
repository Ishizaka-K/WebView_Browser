package com.example.webviewbrowser.history.di

import com.example.webviewbrowser.core.port.HistoryRecorder
import com.example.webviewbrowser.history.domain.HistoryUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** history feature の DI（本タスク所有）。HistoryRecorder の本実装を提供する。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryFeatureModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRecorder(impl: HistoryUseCase): HistoryRecorder
}
