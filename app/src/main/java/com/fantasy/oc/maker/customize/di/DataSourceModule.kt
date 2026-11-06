package com.fantasy.oc.maker.customize.di

import com.fantasy.oc.maker.customize.core.common.AppConfig
import com.fantasy.oc.maker.customize.data.remote.ApiRemoteDataSource
import com.fantasy.oc.maker.customize.data.remote.AssetProbe
import com.fantasy.oc.maker.customize.data.remote.CharacterImageComposer
import com.fantasy.oc.maker.customize.data.remote.GlideCharacterImageComposer
import com.fantasy.oc.maker.customize.data.remote.MakerRemoteDataSource
import com.fantasy.oc.maker.customize.data.remote.MockDataSource
import com.fantasy.oc.maker.customize.data.remote.OkHttpAssetProbe
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
