package com.conght.facemakerapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CreationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creationDao(): CreationDao

    companion object {
        const val NAME = "face_maker.db"
    }
}
