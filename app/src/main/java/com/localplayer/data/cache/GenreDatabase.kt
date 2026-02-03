package com.localplayer.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for storing artist-genre cache.
 * Uses singleton pattern to ensure single instance throughout app.
 */
@Database(
    entities = [ArtistGenreCache::class],
    version = 1,
    exportSchema = false
)
abstract class GenreDatabase : RoomDatabase() {
    
    abstract fun artistGenreCacheDao(): ArtistGenreCacheDao
    
    companion object {
        private const val DATABASE_NAME = "genre_cache.db"
        
        @Volatile
        private var INSTANCE: GenreDatabase? = null
        
        /**
         * Get the singleton database instance.
         * Creates the database if it doesn't exist.
         */
        fun getInstance(context: Context): GenreDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): GenreDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GenreDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
