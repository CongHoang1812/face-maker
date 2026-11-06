package com.conght.facemakerapp.data.model

enum class StickerTarget { WHATSAPP, TELEGRAM }

/** Số sticker WhatsApp cho phép trong một gói. */
const val WHATSAPP_PACK_MIN_STICKERS = 3
const val WHATSAPP_PACK_MAX_STICKERS = 30

/** WhatsApp giới hạn tên gói 128 ký tự (khớp @integer/pack_name_max_length). */
const val PACK_NAME_MAX_LENGTH = 128

/** Kết quả chuẩn bị sticker, UI dùng để mở app đích hoặc báo cho người dùng. */
sealed interface StickerExport {
    /** [packages]: các bản WhatsApp đã cài mà chưa có gói này. */
    data class WhatsApp(val packId: String, val packName: String, val packages: List<String>) : StickerExport

    data class Telegram(val paths: List<String>, val emojis: List<String>) : StickerExport

    data object WhatsAppNotInstalled : StickerExport

    data object WhatsAppAlreadyAdded : StickerExport
}
