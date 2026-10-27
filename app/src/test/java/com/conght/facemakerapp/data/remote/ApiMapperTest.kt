package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.layerUrlFor
import com.conght.facemakerapp.data.remote.dto.DecorCategoryDto
import com.conght.facemakerapp.data.remote.dto.DecorConfigDto
import com.conght.facemakerapp.data.remote.dto.PartDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiMapperTest {

    private val base = "https://host/public/app/X/"

    private val parts = listOf(
        PartDto("11", "30-2", "", 2, 1),
        PartDto("11", "37-3", "000000,ffffff", 4, 1),
        PartDto("11", "1-1", "", 3, 1),
        PartDto("11", "bad", "", 3, 1),
        PartDto("11", "5-5", "", 0, 1),
    )

    @Test
    fun templates_sortedByLevel_withAvatarUrl() {
        val data = mapOf(
            "data_1" to listOf(PartDto("data_1", "1-1", "", 1, 3)),
            "11" to listOf(PartDto("11", "1-1", "", 1, 1)),
            "empty" to emptyList(),
        )
        val templates = ApiMapper.toTemplates(data, base)
        assertEquals(listOf("11", "data_1"), templates.map { it.id })
        assertEquals("${base}11/avatar.png", templates[0].thumbnailUrl)
    }

    @Test
    fun detail_categoriesSortedByTab_invalidSkipped() {
        val detail = ApiMapper.toTemplateDetail("11", parts, base)
        assertEquals(listOf("1-1", "30-2", "37-3"), detail.categories.map { it.id })
        assertEquals(listOf(1, 30, 37), detail.categories.map { it.zIndex })
        assertEquals("${base}11/1-1/nav.png", detail.categories[0].iconUrl)
    }

    @Test
    fun detail_coloredItem_usesColorLayer() {
        val category = ApiMapper.toTemplateDetail("11", parts, base).categories.first { it.id == "37-3" }
        assertEquals(listOf("1", "2", "3", "4"), category.items.map { it.id })
        val item = category.items[1]
        assertEquals("${base}11/37-3/thumb_2.png", item.thumbnailUrl)
        assertEquals(listOf("000000", "FFFFFF"), item.colors.map { it.id })
        assertEquals("#FFFFFF", item.colors[1].hex)
        assertEquals("${base}11/37-3/FFFFFF/2.png", category.layerUrlFor(PartSelection("2", "FFFFFF")))
        assertNull(item.layerUrl)
    }

    @Test
    fun detail_plainItem_usesPlainLayer() {
        val category = ApiMapper.toTemplateDetail("11", parts, base).categories.first { it.id == "1-1" }
        assertTrue(category.items.all { it.colors.isEmpty() })
        assertEquals("${base}11/1-1/3.png", category.layerUrlFor(PartSelection("3", null)))
        assertNull(category.layerUrlFor(PartSelection(null, null)))
    }

    @Test
    fun detail_firstTabIsRequired_andOnlyOneSelectedByDefault() {
        val detail = ApiMapper.toTemplateDetail("11", parts, base)
        val required = detail.categories.filter { it.isRequired }.map { it.id }
        assertEquals(listOf("1-1"), required)
        assertEquals(PartSelection("1", null), detail.defaultConfig.selections["1-1"])
        assertEquals(PartSelection(null, null), detail.defaultConfig.selections["37-3"])
        assertEquals(detail.categories.size, detail.defaultConfig.selections.size)
        // API không trả item None: ô None do UI (NoneFirst) thêm vào
        assertFalse(detail.categories.any { c -> c.items.any { it.id == "0" } })
    }

    @Test
    fun decorPacks_buildUrlsPerType() {
        val config = DecorConfigDto(
            background = listOf(DecorCategoryDto("", 2)),
            sticker = listOf(DecorCategoryDto("Emoji", 3), DecorCategoryDto("Empty", 0)),
            speechBubble = listOf(DecorCategoryDto("1", 1)),
        )
        val bg = ApiMapper.toDecorPacks(config, DecorType.BACKGROUND, base)
        assertEquals("${base}bg/background/2.png", bg.single().items[1].imageUrl)

        val sticker = ApiMapper.toDecorPacks(config, DecorType.STICKER, base)
        assertEquals(1, sticker.size)
        assertEquals("Emoji", sticker[0].name)
        assertEquals("${base}bg/sticker/Emoji/3.png", sticker[0].items[2].imageUrl)

        val speech = ApiMapper.toDecorPacks(config, DecorType.SPEECH, base).single()
        assertEquals("", speech.name)
        assertEquals("${base}bg/speech%20bubble/1/1.png", speech.items[0].imageUrl)
        assertEquals(DecorType.SPEECH, speech.type)
    }
}
