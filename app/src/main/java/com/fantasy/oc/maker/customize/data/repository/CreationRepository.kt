package com.fantasy.oc.maker.customize.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.fantasy.oc.maker.customize.core.common.DispatcherProvider
import com.fantasy.oc.maker.customize.data.local.db.CreationDao
import com.fantasy.oc.maker.customize.data.local.db.CreationEntity
import com.fantasy.oc.maker.customize.data.local.db.toModel
import com.fantasy.oc.maker.customize.data.local.file.ImageFileStore
import com.fantasy.oc.maker.customize.data.model.CharacterConfig
import com.fantasy.oc.maker.customize.data.model.Creation
import com.fantasy.oc.maker.customize.data.model.CreationType
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface CreationRepository {
    fun observe(type: CreationType): Flow<List<Creation>>
    suspend fun get(id: Long): Creation?

    /** Lưu ảnh vào internal storage + Room, trả về id. */
    suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig? = null): Long

    /** Xoá bản ghi và file ảnh. */
    suspend fun delete(ids: List<Long>)

    /** Lưu ảnh ra thư viện máy (Gallery). */
    suspend fun download(ids: List<Long>): Result<Unit>

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
            )
        }

    override suspend fun delete(ids: List<Long>) = withContext(dispatchers.io) {
        val entities = dao.getByIds(ids)
        dao.deleteByIds(ids)
        entities.forEach { fileStore.delete(it.imagePath) }
    }

    override suspend fun saveTemp(bitmap: Bitmap, fileName: String): String = fileStore.saveTemp(bitmap, fileName)

    override suspend fun importImage(uri: Uri): String = fileStore.importImage(uri)

    override suspend fun download(ids: List<Long>): Result<Unit> = withContext(dispatchers.io) {
        try {
            dao.getByIds(ids).forEach { fileStore.copyToGallery(it.imagePath) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
