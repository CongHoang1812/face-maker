package com.conght.facemakerapp.core.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.conght.facemakerapp.data.local.file.StickerPackStore
import com.conght.facemakerapp.data.model.StickerExport
import java.io.File

/** Mở WhatsApp thêm gói; máy có cả WhatsApp và WhatsApp Business thì cho người dùng chọn. */
fun Context.whatsAppAddStickerIntent(export: StickerExport.WhatsApp, chooserTitle: CharSequence): Intent {
    val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK")
        .putExtra("sticker_pack_id", export.packId)
        .putExtra("sticker_pack_authority", StickerPackStore.authority(packageName))
        .putExtra("sticker_pack_name", export.packName)
    return export.packages.singleOrNull()?.let(intent::setPackage) ?: Intent.createChooser(intent, chooserTitle)
}

/** Gửi sticker sang Telegram (cả bản web, beta). Trả về false nếu máy không có Telegram. */
fun Context.startTelegramStickerImport(export: StickerExport.Telegram): Boolean {
    val uris: List<Uri> = try {
        export.paths.map { FileProvider.getUriForFile(this, "$packageName.fileprovider", File(it)) }
    } catch (_: IllegalArgumentException) {
        return false
    }
    val intent = Intent("org.telegram.messenger.CREATE_STICKER_PACK")
        .setType("image/*")
        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        .putStringArrayListExtra("STICKER_EMOJIS", ArrayList(export.emojis))
        .putExtra("IMPORTER", packageName)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // ClipData để quyền đọc được cấp cho mọi Uri trong EXTRA_STREAM
    intent.clipData = ClipData.newUri(contentResolver, null, uris.first()).also { clip ->
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    }
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
