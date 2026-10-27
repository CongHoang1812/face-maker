package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.remote.dto.DecorCategoryDto
import com.conght.facemakerapp.data.remote.dto.DecorConfigDto
import com.conght.facemakerapp.data.remote.dto.PartDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

class ApiRemoteDataSourceTest {

    private class FakeApi(var fail: Boolean = false) : ApiService {
        var dataCalls = 0
        var decorCalls = 0

        override suspend fun getAllData(): Map<String, List<PartDto>> {
            dataCalls++
            if (fail) throw IOException("offline")
            return mapOf(
                "11" to listOf(PartDto("11", "2-2", "000000,FFFFFF", 3, 1), PartDto("11", "1-1", "", 2, 1)),
            )
        }

        override suspend fun getDecorConfig(): DecorConfigDto {
            decorCalls++
            if (fail) throw IOException("offline")
            return DecorConfigDto(listOf(DecorCategoryDto("", 2)), emptyList(), emptyList())
        }
    }

    private val composed = mutableListOf<String>()
    private val composer = CharacterImageComposer { _, _, prefix -> "/cache/$prefix.png".also(composed::add) }

    /** Host bị chặn ảnh (server trả 302 → /404.html). */
    private val blockedHosts = mutableSetOf<String>()
    private val probe = AssetProbe { url -> blockedHosts.none { url.startsWith(it) } }

    private fun source(primary: FakeApi, backup: FakeApi = FakeApi()) =
        ApiRemoteDataSource(primary, backup, composer, CharacterRandomizer(Random(1)), probe)

    @Test
    fun templates_fromPrimary_cachedAcrossCalls() = runTest {
        val primary = FakeApi()
        val source = source(primary)
        val templates = source.getTemplates()
        source.getTemplateDetail("11")
        assertEquals(listOf("11"), templates.map { it.id })
        assertTrue(templates[0].thumbnailUrl.startsWith(AppConfig.BASE_URL))
        assertEquals(1, primary.dataCalls)
    }

    @Test
    fun primaryFails_usesBackupAndBackupImageHost() = runTest {
        val backup = FakeApi()
        val source = source(FakeApi(fail = true), backup)
        val detail = source.getTemplateDetail("11")
        assertEquals(1, backup.dataCalls)
        assertTrue(detail.categories[0].iconUrl.startsWith(AppConfig.BACKUP_BASE_URL + AppConfig.ASSET_PATH))
    }

    @Test(expected = IOException::class)
    fun bothFail_throwsIOException() = runTest {
        source(FakeApi(fail = true), FakeApi(fail = true)).getTemplates()
    }

    @Test
    fun failedLoad_isRetriedNextTime() = runTest {
        val primary = FakeApi(fail = true)
        val source = source(primary, primary)
        runCatching { source.getTemplates() }
        primary.fail = false
        assertEquals(1, source.getTemplates().size)
    }

    @Test
    fun primaryImagesBlocked_usesBackup() = runTest {
        blockedHosts += AppConfig.BASE_URL
        val backup = FakeApi()
        val templates = source(FakeApi(), backup).getTemplates()
        assertEquals(1, backup.dataCalls)
        assertTrue(templates[0].thumbnailUrl.startsWith(AppConfig.BACKUP_BASE_URL))
    }

    @Test(expected = AssetUnavailableException::class)
    fun allImagesBlocked_throwsNonNetworkError() = runTest {
        blockedHosts += listOf(AppConfig.BASE_URL, AppConfig.BACKUP_BASE_URL)
        source(FakeApi(), FakeApi()).getTemplates()
    }

    @Test
    fun decorImages_alwaysFromPrimaryHost() = runTest {
        val packs = source(FakeApi(fail = true), FakeApi()).getDecorPacks(DecorType.BACKGROUND)
        assertTrue(packs.single().items[0].imageUrl.startsWith(AppConfig.BASE_URL + AppConfig.ASSET_PATH))
    }

    @Test
    fun randomizeCharacter_requiredAlwaysSelected_validIds() = runTest {
        val source = source(FakeApi())
        val detail = source.getTemplateDetail("11")
        repeat(20) {
            val config = source.randomizeCharacter("11")
            val body = config.selections.getValue("1-1")
            assertNotNull(body.itemId)
            detail.categories.forEach { category ->
                val selection = config.selections.getValue(category.id)
                selection.itemId?.let { id ->
                    val item = category.items.first { it.id == id }
                    assertTrue(selection.colorId == null || item.colors.any { it.id == selection.colorId })
                }
            }
        }
    }

    @Test
    fun randomizeCategory_returnsItemOfThatCategory() = runTest {
        val selection = source(FakeApi()).randomizeCategory("11", "2-2")
        assertTrue(selection!!.itemId in listOf("1", "2", "3"))
        assertTrue(selection.colorId in listOf("000000", "FFFFFF"))
    }

    @Test
    fun cosplayAndLuckyRoll_useComposedImage() = runTest {
        val source = source(FakeApi())
        val challenge = source.getRandomCosplayChallenge()
        val roll = source.getLuckyRoll()
        assertEquals("11", challenge.templateId)
        assertEquals("/cache/cosplay_reference.png", challenge.referenceImageUrl)
        assertEquals(2, challenge.answer.selections.size)
        assertEquals("/cache/lucky_roll.png", roll.imageUrl)
        assertEquals(2, composed.size)
    }
}
