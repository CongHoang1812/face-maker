package com.conght.facemakerapp.data.remote

import android.util.Log
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import kotlinx.coroutines.awaitCancellation
import javax.inject.Inject

/**
 * Implementation hiện tại khi CHƯA có API: các hàm lấy dữ liệu treo vô hạn
 * (UI luôn ở trạng thái Loading -> shimmer).
 */
@Suppress("unused") // apiService sẽ dùng khi có API
class PendingRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
) : MakerRemoteDataSource {

    // TODO(API): gọi apiService.getTemplates()
    override suspend fun getTemplates(): List<CharacterTemplate> = awaitCancellation()

    // TODO(API): gọi apiService.getTemplateDetail(templateId)
    override suspend fun getTemplateDetail(templateId: String): TemplateDetail = awaitCancellation()

    // TODO(API): gọi apiService.getDecorPacks(type)
    override suspend fun getDecorPacks(type: DecorType): List<DecorPack> = awaitCancellation()

    // TODO(API): gọi apiService.getRandomCosplayChallenge()
    override suspend fun getRandomCosplayChallenge(): CosplayChallenge = awaitCancellation()

    // TODO(API): gọi apiService.getLuckyRoll()
    override suspend fun getLuckyRoll(): LuckyRollResult = awaitCancellation()

    override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? {
        // TODO(API): gọi apiService.randomizeCategory(templateId, categoryId)
        Log.d(TAG, "TODO(API): randomizeCategory($templateId, $categoryId)")
        return null
    }

    override suspend fun randomizeCharacter(templateId: String): CharacterConfig? {
        // TODO(API): gọi apiService.randomizeCharacter(templateId)
        Log.d(TAG, "TODO(API): randomizeCharacter($templateId)")
        return null
    }

    private companion object {
        const val TAG = "PendingRemoteDataSource"
    }
}
