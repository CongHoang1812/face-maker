package com.fantasy.oc.maker.customize.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CreationEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creationDao(): CreationDao

    companion object {
        const val NAME = "face_maker.db"

        /** Schema giữ nguyên, chỉ dọn item mẫu của bản debug cũ: ảnh rỗng nên ô ảnh shimmer mãi. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM creations WHERE image_path = ''")
            }
        }
    }
}
