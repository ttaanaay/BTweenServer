package com.btween.app.di

import android.content.Context
import androidx.room.Room
import com.btween.app.data.local.dao.CategoryDao
import com.btween.app.data.local.dao.QuoteDao
import com.btween.app.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        @ApplicationScope applicationScope: CoroutineScope
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addCallback(AppDatabase.SeedCallback(categoryDaoProvider, applicationScope))
            .build()
    }

    @Provides
    @Singleton
    fun provideQuoteDao(database: AppDatabase): QuoteDao = database.quoteDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()
}
