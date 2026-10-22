package com.conght.facemakerapp.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreationDao {

    @Query("SELECT * FROM creations WHERE type = :type ORDER BY created_at DESC")
    fun observeByType(type: String): Flow<List<CreationEntity>>

    @Query("SELECT * FROM creations WHERE id = :id")
    suspend fun getById(id: Long): CreationEntity?

    @Query("SELECT * FROM creations WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CreationEntity>

    @Insert
    suspend fun insert(entity: CreationEntity): Long

    @Query("DELETE FROM creations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
