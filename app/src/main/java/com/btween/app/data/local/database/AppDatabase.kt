package com.btween.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.btween.app.data.local.converter.TagsConverter
import com.btween.app.data.local.dao.CategoryDao
import com.btween.app.data.local.dao.QuoteDao
import com.btween.app.data.local.entity.CategoryEntity
import com.btween.app.data.local.entity.QuoteEntity
import com.btween.app.domain.model.DefaultCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider

@Database(
    entities = [QuoteEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(TagsConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quoteDao(): QuoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "btween_database"
    }

    /**
     * Populates the nine default categories the very first time the database file is
     * created on-device. Runs once; subsequent app launches skip this entirely since
     * `onCreate` is only invoked when the underlying SQLite file doesn't yet exist.
     */
    class SeedCallback(
        private val categoryDaoProvider: Provider<CategoryDao>,
        private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            applicationScope.launch {
                val defaultEntities = DefaultCategories.seed.map { (name, color) ->
                    CategoryEntity(name = name, colorHex = color, isDefault = true)
                }
                categoryDaoProvider.get().insertAll(defaultEntities)
            }
        }
    }
}
