package com.conght.facemakerapp.di

import com.conght.facemakerapp.data.repository.CosplayRepository
import com.conght.facemakerapp.data.repository.CosplayRepositoryImpl
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.CreationRepositoryImpl
import com.conght.facemakerapp.data.repository.DecorRepository
import com.conght.facemakerapp.data.repository.DecorRepositoryImpl
import com.conght.facemakerapp.data.repository.LuckyRollRepository
import com.conght.facemakerapp.data.repository.LuckyRollRepositoryImpl
import com.conght.facemakerapp.data.repository.MakerRepository
import com.conght.facemakerapp.data.repository.MakerRepositoryImpl
import com.conght.facemakerapp.data.repository.SettingsRepository
import com.conght.facemakerapp.data.repository.SettingsRepositoryImpl
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
}
