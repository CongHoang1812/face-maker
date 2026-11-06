package com.fantasy.oc.maker.customize.di

import com.fantasy.oc.maker.customize.data.repository.CosplayRepository
import com.fantasy.oc.maker.customize.data.repository.CosplayRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.CreationRepository
import com.fantasy.oc.maker.customize.data.repository.CreationRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.DecorRepository
import com.fantasy.oc.maker.customize.data.repository.DecorRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.LuckyRollRepository
import com.fantasy.oc.maker.customize.data.repository.LuckyRollRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.MakerRepository
import com.fantasy.oc.maker.customize.data.repository.MakerRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.SettingsRepository
import com.fantasy.oc.maker.customize.data.repository.SettingsRepositoryImpl
import com.fantasy.oc.maker.customize.data.repository.StickerRepository
import com.fantasy.oc.maker.customize.data.repository.StickerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindMakerRepository(impl: MakerRepositoryImpl): MakerRepository

    @Binds @Singleton
    abstract fun bindDecorRepository(impl: DecorRepositoryImpl): DecorRepository

    @Binds @Singleton
    abstract fun bindCosplayRepository(impl: CosplayRepositoryImpl): CosplayRepository

    @Binds @Singleton
    abstract fun bindLuckyRollRepository(impl: LuckyRollRepositoryImpl): LuckyRollRepository

    @Binds @Singleton
    abstract fun bindCreationRepository(impl: CreationRepositoryImpl): CreationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository
}
