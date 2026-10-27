package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.DecorItem
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.model.PartCategory
import com.conght.facemakerapp.data.model.PartColor
import com.conght.facemakerapp.data.model.PartItem
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.remote.dto.DecorCategoryDto
import com.conght.facemakerapp.data.remote.dto.DecorConfigDto
import com.conght.facemakerapp.data.remote.dto.PartDto
import java.net.URLEncoder

/**
 * DTO → model. API không trả link ảnh: link được ghép theo mẫu trong tài liệu API, tính từ
 * [assetBase] (`{BASE_URL}{ASSET_PATH}`):
 * - avatar template `{key}/avatar.png`
 * - icon tab `{position}/{parts}/nav.png`, thumbnail `{position}/{parts}/thumb_{i}.png`
 * - layer `{position}/{parts}/{màu}/{i}.png` (có màu) hoặc `{position}/{parts}/{i}.png`
 * - decor `bg/background[/{category}]/{i}.png`, `bg/sticker/{category}/{i}.png`,
 *   `bg/speech bubble/{category}/{i}.png`
 *
 * `parts` = `X-Y`: X = zIndex (lớp vẽ), Y = thứ tự tab. Item id = số thứ tự `i` (1..quantity),
 * color id = mã hex. Category bắt buộc là tab đầu (thân nhân vật); config mặc định chỉ chọn item đầu
 * của category đó, các category khác None.
 */
object ApiMapper {

    fun toTemplates(data: Map<String, List<PartDto>>, assetBase: String): List<CharacterTemplate> =
        data.filterValues { parts -> parts.any { it.parsedParts() != null } }
            .entries
            .sortedBy { (_, parts) -> parts.minOfOrNull { it.level ?: Int.MAX_VALUE } ?: Int.MAX_VALUE }
            .map { (key, _) -> CharacterTemplate(id = key, name = key, thumbnailUrl = "$assetBase${key.encodePath()}/avatar.png") }

    fun toTemplateDetail(templateId: String, parts: List<PartDto>, assetBase: String): TemplateDetail {
        val categories = parts
            .mapNotNull { dto -> dto.parsedParts()?.let { dto to it } }
            .sortedBy { (_, parsed) -> parsed.tabOrder }
            .map { (dto, parsed) -> toCategory(templateId, dto, parsed, assetBase) }
        val withRequired = categories.mapIndexed { index, category -> category.copy(isRequired = index == 0) }
        return TemplateDetail(
            templateId = templateId,
            categories = withRequired,
            defaultConfig = CharacterConfig(
                templateId = templateId,
                selections = withRequired.associate { category ->
                    val item = category.items.firstOrNull()?.takeIf { category.isRequired }
                    category.id to PartSelection(item?.id, item?.colors?.firstOrNull()?.id)
                },
            ),
        )
    }

    fun toDecorPacks(config: DecorConfigDto, type: DecorType, assetBase: String): List<DecorPack> {
        val (groups, folder) = when (type) {
            DecorType.BACKGROUND -> config.background to "bg/background"
            DecorType.STICKER -> config.sticker to "bg/sticker"
            DecorType.SPEECH -> config.speechBubble to "bg/speech bubble"
        }
        val prefix = type.name.lowercase()
        return groups.orEmpty()
            .filter { (it.quantity ?: 0) > 0 }
            .mapIndexed { index, group -> toDecorPack(group, type, "${prefix}_$index", "$assetBase${folder.encodePath()}") }
    }

    private fun toDecorPack(group: DecorCategoryDto, type: DecorType, packId: String, folderUrl: String): DecorPack {
        val category = group.category.orEmpty().trim()
        val dir = if (category.isEmpty()) folderUrl else "$folderUrl/${category.encodePath()}"
        return DecorPack(
            id = packId,
            // Tên nhóm chỉ là số ("1", "2"…) → để trống, UI tự hiện "Pack N"
            name = category.takeUnless { name -> name.all(Char::isDigit) }.orEmpty(),
            type = type,
            items = (1..(group.quantity ?: 0)).map { i ->
                val url = "$dir/$i.png"
                DecorItem(id = "${packId}_$i", thumbnailUrl = url, imageUrl = url)
            },
        )
    }

    private fun toCategory(templateId: String, dto: PartDto, parsed: ParsedParts, assetBase: String): PartCategory {
        val position = dto.position?.takeIf { it.isNotBlank() } ?: templateId
        val dir = "$assetBase${position.encodePath()}/${parsed.raw.encodePath()}"
        val hexes = dto.colorArray.orEmpty().split(',').map { it.trim().removePrefix("#").uppercase() }.filter { it.isNotEmpty() }.distinct()
        return PartCategory(
            id = parsed.raw,
            name = parsed.raw,
            iconUrl = "$dir/nav.png",
            zIndex = parsed.zIndex,
            isRequired = false,
            items = (1..(dto.quantity ?: 0)).map { i ->
                PartItem(
                    id = i.toString(),
                    thumbnailUrl = "$dir/thumb_$i.png",
                    colors = hexes.map { hex -> PartColor(id = hex, hex = "#$hex", layerUrl = "$dir/$hex/$i.png") },
                    layerUrl = if (hexes.isEmpty()) "$dir/$i.png" else null,
                )
            },
        )
    }

    private data class ParsedParts(val raw: String, val tabOrder: Int, val zIndex: Int)

    private fun PartDto.parsedParts(): ParsedParts? {
        val raw = parts?.trim() ?: return null
        if ((quantity ?: 0) <= 0) return null
        // Tài liệu API ghi ngược: kiểm tra thực tế, X là lớp vẽ, Y là thứ tự tab (Y = 1 luôn là thân)
        val z = raw.substringBefore('-').toIntOrNull() ?: return null
        val tab = raw.substringAfter('-', "").toIntOrNull() ?: return null
        return ParsedParts(raw, tab, z)
    }

    /** Mã hoá 1 hoặc nhiều đoạn path (giữ `/`, dấu cách → `%20`). */
    private fun String.encodePath(): String =
        split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
}
