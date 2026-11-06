package com.fantasy.oc.maker.customize.data.remote

import com.fantasy.oc.maker.customize.core.ui.MockImageLoader.MOCK_COLOR_PREFIX
import com.fantasy.oc.maker.customize.data.model.CharacterConfig
import com.fantasy.oc.maker.customize.data.model.CharacterTemplate
import com.fantasy.oc.maker.customize.data.model.CosplayChallenge
import com.fantasy.oc.maker.customize.data.model.DecorItem
import com.fantasy.oc.maker.customize.data.model.DecorPack
import com.fantasy.oc.maker.customize.data.model.DecorType
import com.fantasy.oc.maker.customize.data.model.LuckyRollResult
import com.fantasy.oc.maker.customize.data.model.PartCategory
import com.fantasy.oc.maker.customize.data.model.PartColor
import com.fantasy.oc.maker.customize.data.model.PartItem
import com.fantasy.oc.maker.customize.data.model.PartSelection
import com.fantasy.oc.maker.customize.data.model.TemplateDetail
import kotlinx.coroutines.delay
import javax.inject.Inject

/** Dữ liệu giả để test UI, chỉ dùng khi AppConfig.USE_MOCK_DATA = true. */
class MockDataSource @Inject constructor(
    private val randomizer: CharacterRandomizer,
) : MakerRemoteDataSource {

    override suspend fun getTemplates(): List<CharacterTemplate> {
        delay(DELAY_MS)
        return List(TEMPLATE_COUNT) { i ->
            CharacterTemplate(id = "template_$i", name = "Template ${i + 1}", thumbnailUrl = colorUrl(i))
        }
    }

    override suspend fun getTemplateDetail(templateId: String): TemplateDetail {
        delay(DELAY_MS)
        val categories = List(CATEGORY_COUNT) { c ->
            PartCategory(
                id = "category_$c",
                name = "Category ${c + 1}",
                iconUrl = colorUrl(c),
                zIndex = c,
                isRequired = false,
                items = List(ITEM_COUNT) { i ->
                    PartItem(
                        id = "item_${c}_$i",
                        thumbnailUrl = colorUrl(c + i),
                        colors = PALETTE.mapIndexed { k, hex ->
                            PartColor(id = "color_$k", hex = hex, layerUrl = MOCK_COLOR_PREFIX + hex)
                        },
                    )
                },
            )
        }
        return TemplateDetail(
            templateId = templateId,
            categories = categories,
            defaultConfig = CharacterConfig(
                templateId = templateId,
                selections = categories.associate { it.id to PartSelection(itemId = null, colorId = null) },
            ),
        )
    }

    override suspend fun getDecorPacks(type: DecorType): List<DecorPack> {
        delay(DELAY_MS)
        return List(PACK_COUNT) { p ->
            DecorPack(
                id = "${type.name.lowercase()}_pack_$p",
                name = "Pack ${p + 1}",
                type = type,
                items = List(ITEM_COUNT) { i ->
                    val url = colorUrl(p + i)
                    DecorItem(id = "${type.name.lowercase()}_${p}_$i", thumbnailUrl = url, imageUrl = url)
                },
            )
        }
    }

    override suspend fun getRandomCosplayChallenge(): CosplayChallenge {
        delay(DELAY_MS)
        val templateId = "template_0"
        return CosplayChallenge(
            id = "challenge_0",
            referenceImageUrl = colorUrl(0),
            templateId = templateId,
            answer = CharacterConfig(
                templateId = templateId,
                selections = List(CATEGORY_COUNT) { c -> "category_$c" to PartSelection("item_${c}_0", null) }.toMap(),
            ),
        )
    }

    override suspend fun getLuckyRoll(): LuckyRollResult {
        delay(DELAY_MS)
        return LuckyRollResult(
            imageUrl = colorUrl(1),
            config = CharacterConfig(templateId = "template_0", selections = emptyMap()),
        )
    }

    override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? =
        getTemplateDetail(templateId).categories.firstOrNull { it.id == categoryId }?.let(randomizer::randomSelection)

    override suspend fun randomizeCharacter(templateId: String): CharacterConfig =
        randomizer.randomConfig(getTemplateDetail(templateId))

    private fun colorUrl(index: Int) = MOCK_COLOR_PREFIX + PALETTE[index % PALETTE.size]

    private companion object {
        const val DELAY_MS = 1500L
        const val TEMPLATE_COUNT = 6
        const val CATEGORY_COUNT = 6
        const val ITEM_COUNT = 20
        const val PACK_COUNT = 3
        val PALETTE = listOf("#E57373", "#FFB74D", "#FFF176", "#81C784", "#4FC3F7", "#7986CB", "#BA68C8", "#A1887F")
    }
}
