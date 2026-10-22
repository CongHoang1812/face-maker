package com.conght.facemakerapp.di

import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import com.conght.facemakerapp.data.remote.MockDataSource
import com.conght.facemakerapp.data.remote.PendingRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    // TODO(API): khi có API thật, thay PendingRemoteDataSource bằng implementation gọi ApiService
    @Provides
    @Singleton
    fun provideMakerRemoteDataSource(
        mock: Provider<MockDataSource>,
        pending: Provider<PendingRemoteDataSource>,
    ): MakerRemoteDataSource = if (AppConfig.USE_MOCK_DATA) mock.get() else pending.get()
}
