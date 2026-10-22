package com.conght.facemakerapp.data.local.db

import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.data.local.datastore.SettingsDataStore
import com.conght.facemakerapp.data.model.CreationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chèn 1 lần vài item mẫu vào My creation (chỉ khi [AppConfig.SEED_SAMPLE_CREATIONS]) để xem giao diện.
 * Item mẫu chưa có file ảnh → ô ảnh chạy shimmer như thẻ chờ API ở màn chọn template.
 * Xoá item mẫu trong app thì không bị chèn lại.
 */
@Singleton
class SampleCreationSeeder @Inject constructor(
    private val dao: CreationDao,
    private val settings: SettingsDataStore,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun seedIfNeeded() = withContext(dispatchers.io) {
        if (!AppConfig.SEED_SAMPLE_CREATIONS || settings.sampleCreationsSeeded.first()) return@withContext
        val now = System.currentTimeMillis()
        CreationType.entries.forEach { type ->
            repeat(SAMPLES_PER_TYPE) { i ->
                dao.insert(
                    CreationEntity(
                        type = type.name,
                        imagePath = "", // chưa có ảnh
                        configJson = null,
                        createdAt = now - i * SAMPLE_INTERVAL_MS,
                    ),
                )
            }
        }
        settings.setSampleCreationsSeeded()
    }

    private companion object {
        const val SAMPLES_PER_TYPE = 6
        const val SAMPLE_INTERVAL_MS = 60_000L
    }
}
