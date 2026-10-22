package com.conght.facemakerapp.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.data.local.db.CreationDao
import com.conght.facemakerapp.data.local.db.CreationEntity
import com.conght.facemakerapp.data.local.db.toModel
import com.conght.facemakerapp.data.local.file.ImageFileStore
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

interface CreationRepository {
    fun observe(type: CreationType): Flow<List<Creation>>
    suspend fun get(id: Long): Creation?

    /** Lưu ảnh vào internal storage + Room, trả về id. */
    suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig? = null): Long

    /** Xoá bản ghi và file ảnh. */
    suspend fun delete(ids: List<Long>)

    /** Copy ảnh vào internal storage `filesDir/downloads/` (không ra Gallery). */
    suspend fun download(ids: List<Long>): Result<List<File>>

    /** Ảnh tạm truyền giữa các màn (cacheDir, internal), trả về đường dẫn. */
    suspend fun saveTemp(bitmap: Bitmap, fileName: String): String

    /** Copy ảnh người dùng chọn (Photo Picker) vào cacheDir (internal), trả về đường dẫn. */
    suspend fun importImage(uri: Uri): String
}

class CreationRepositoryImpl @Inject constructor(
    private val dao: CreationDao,
    private val fileStore: ImageFileStore,
    private val gson: Gson,
    private val dispatchers: DispatcherProvider,
    private val settingsRepository: SettingsRepository,
) : CreationRepository {

    override fun observe(type: CreationType): Flow<List<Creation>> =
        dao.observeByType(type.name)
            .map { list -> list.map { it.toModel() } }
            .flowOn(dispatchers.io)

    override suspend fun get(id: Long): Creation? = withContext(dispatchers.io) {
        dao.getById(id)?.toModel()
    }

    override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?): Long =
        withContext(dispatchers.io) {
            val path = fileStore.saveCreation(bitmap)
            dao.insert(
                CreationEntity(
                    type = type.name,
                    imagePath = path,
                    configJson = config?.let(gson::toJson),
                    createdAt = System.currentTimeMillis(),
                ),
            ).also { settingsRepository.incrementCreationSaveCount() }
        }

    override suspend fun delete(ids: List<Long>) = withContext(dispatchers.io) {
        val entities = dao.getByIds(ids)
        dao.deleteByIds(ids)
        entities.forEach { fileStore.delete(it.imagePath) }
    }

    override suspend fun saveTemp(bitmap: Bitmap, fileName: String): String = fileStore.saveTemp(bitmap, fileName)

    override suspend fun importImage(uri: Uri): String = fileStore.importImage(uri)

    override suspend fun download(ids: List<Long>): Result<List<File>> = withContext(dispatchers.io) {
        try {
            Result.success(dao.getByIds(ids).map { fileStore.copyToDownloads(it.imagePath) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
