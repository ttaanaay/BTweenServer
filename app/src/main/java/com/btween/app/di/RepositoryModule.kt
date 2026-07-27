package com.btween.app.di

import com.btween.app.data.repository.AuthRepositoryImpl
import com.btween.app.data.repository.CategoryRepositoryImpl
import com.btween.app.data.repository.QuoteRepositoryImpl
import com.btween.app.data.repository.SettingsRepositoryImpl
import com.btween.app.domain.repository.AuthRepository
import com.btween.app.domain.repository.CategoryRepository
import com.btween.app.domain.repository.QuoteRepository
import com.btween.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
