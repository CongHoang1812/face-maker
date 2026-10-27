package com.conght.facemakerapp.data.remote

import android.util.Log
import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.remote.dto.DecorConfigDto
import com.conght.facemakerapp.data.remote.dto.PartDto
import com.conght.facemakerapp.di.BackupApi
import com.conght.facemakerapp.di.PrimaryApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gọi API thật. Máy chủ chính lỗi / quá thời gian → thử máy chủ dự phòng (ảnh nhân vật lấy từ
 * máy chủ đã trả dữ liệu; ảnh nền / sticker luôn từ máy chủ chính). Máy chủ trả JSON nhưng chặn file
 * ảnh ([AssetProbe]) cũng coi là lỗi để chuyển máy chủ / hiện Retry. Dữ liệu được giữ trong bộ
 * nhớ suốt phiên (không lưu Room): lần tải lỗi sẽ được gọi lại ở lần sau.
 *
 * API không có endpoint random / cosplay / lucky roll: các phần này random từ dữ liệu template
 * ([CharacterRandomizer]) và ghép ảnh bằng [CharacterImageComposer].
 */
@Singleton
class ApiRemoteDataSource @Inject constructor(
    @param:PrimaryApi private val primary: ApiService,
    @param:BackupApi private val backup: ApiService,
    private val composer: CharacterImageComposer,
    private val randomizer: CharacterRandomizer,
    private val assetProbe: AssetProbe,
) : MakerRemoteDataSource {

    private class CharacterData(val templates: Map<String, List<PartDto>>, val assetBase: String)

    private val mutex = Mutex()
    private val decorMutex = Mutex()
    private var characterData: CharacterData? = null
    private var decorConfig: DecorConfigDto? = null
    private val details = mutableMapOf<String, TemplateDetail>()

    override suspend fun getTemplates(): List<CharacterTemplate> {
        val data = characterData()
        return ApiMapper.toTemplates(data.templates, data.assetBase)
    }

    override suspend fun getTemplateDetail(templateId: String): TemplateDetail {
        val data = characterData()
        return mutex.withLock {
            details.getOrPut(templateId) {
                val parts = data.templates[templateId] ?: throw NoSuchElementException("Không có template $templateId")
                ApiMapper.toTemplateDetail(templateId, parts, data.assetBase)
            }
        }
    }

    override suspend fun getDecorPacks(type: DecorType): List<DecorPack> {
        val config = decorMutex.withLock {
            decorConfig ?: withFallback { api, _ -> api.getDecorConfig() }.also { decorConfig = it }
        }
        return ApiMapper.toDecorPacks(config, type, AppConfig.BASE_URL + AppConfig.ASSET_PATH)
    }

    override suspend fun getRandomCosplayChallenge(): CosplayChallenge {
        val (detail, answer) = randomCharacter()
        return CosplayChallenge(
            id = "${detail.templateId}_${System.currentTimeMillis()}",
            referenceImageUrl = composer.compose(detail, answer, PREFIX_COSPLAY),
            templateId = detail.templateId,
            answer = answer,
        )
    }

    override suspend fun getLuckyRoll(): LuckyRollResult {
        val (detail, config) = randomCharacter()
        return LuckyRollResult(imageUrl = composer.compose(detail, config, PREFIX_LUCKY), config = config)
    }

    override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? {
        val category = getTemplateDetail(templateId).categories.firstOrNull { it.id == categoryId } ?: return null
        return randomizer.randomSelection(category)
    }

    override suspend fun randomizeCharacter(templateId: String): CharacterConfig =
        randomizer.randomConfig(getTemplateDetail(templateId))

    private suspend fun randomCharacter(): Pair<TemplateDetail, CharacterConfig> {
        val template = randomizer.pick(getTemplates()) ?: throw NoSuchElementException("API không có template")
        val detail = getTemplateDetail(template.id)
        return detail to randomizer.randomConfig(detail)
    }

    private suspend fun characterData(): CharacterData = mutex.withLock {
        characterData ?: withFallback { api, baseUrl ->
            val assetBase = baseUrl + AppConfig.ASSET_PATH
            val templates = api.getAllData()
            val sample = ApiMapper.toTemplates(templates, assetBase).firstOrNull()?.thumbnailUrl
            if (sample != null && !assetProbe.isAvailable(sample)) throw AssetUnavailableException(sample)
            CharacterData(templates, assetBase)
        }.also { characterData = it }
    }

    private suspend fun <T> withFallback(block: suspend (api: ApiService, baseUrl: String) -> T): T =
        try {
            block(primary, AppConfig.BASE_URL)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Máy chủ chính lỗi, chuyển sang máy chủ dự phòng", e)
            block(backup, AppConfig.BACKUP_BASE_URL)
        }

    private companion object {
        const val TAG = "ApiRemoteDataSource"
        const val PREFIX_COSPLAY = "cosplay_reference"
        const val PREFIX_LUCKY = "lucky_roll"
    }
}
