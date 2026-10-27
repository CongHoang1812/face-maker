package com.conght.facemakerapp.di

import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.remote.ApiRemoteDataSource
import com.conght.facemakerapp.data.remote.AssetProbe
import com.conght.facemakerapp.data.remote.CharacterImageComposer
import com.conght.facemakerapp.data.remote.GlideCharacterImageComposer
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import com.conght.facemakerapp.data.remote.MockDataSource
import com.conght.facemakerapp.data.remote.OkHttpAssetProbe
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideMakerRemoteDataSource(
        mock: Provider<MockDataSource>,
        api: Provider<ApiRemoteDataSource>,
    ): MakerRemoteDataSource = if (AppConfig.USE_MOCK_DATA) mock.get() else api.get()

    @Provides
    fun provideCharacterImageComposer(impl: GlideCharacterImageComposer): CharacterImageComposer = impl

    @Provides
    fun provideAssetProbe(impl: OkHttpAssetProbe): AssetProbe = impl
}
