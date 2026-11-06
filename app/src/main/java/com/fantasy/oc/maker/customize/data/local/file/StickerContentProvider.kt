package com.fantasy.oc.maker.customize.data.local.file

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.fantasy.oc.maker.customize.core.common.AppConfig
import java.io.FileNotFoundException

/**
 * Giao diện WhatsApp đọc gói sticker. Đường dẫn, tên cột và thứ tự cột theo mẫu chính thức
 * (github.com/WhatsApp/stickers), không được đổi. Chỉ WhatsApp có quyền đọc (xem AndroidManifest).
 */
class StickerContentProvider : ContentProvider() {

    private lateinit var store: StickerPackStore
    private lateinit var authority: String
    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        val context = checkNotNull(context)
        store = StickerPackStore(context)
        authority = StickerPackStore.authority(context.packageName)
        matcher.addURI(authority, "metadata", CODE_ALL_PACKS)
        matcher.addURI(authority, "metadata/*", CODE_SINGLE_PACK)
        matcher.addURI(authority, "stickers/*", CODE_STICKERS)
        matcher.addURI(authority, "stickers_asset/*/*", CODE_ASSET)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = when (matcher.match(uri)) {
        CODE_ALL_PACKS -> packsCursor(store.packs())
        CODE_SINGLE_PACK -> packsCursor(listOfNotNull(uri.lastPathSegment?.let(store::pack)))
        CODE_STICKERS -> stickersCursor(uri.lastPathSegment?.let(store::pack))
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    private fun packsCursor(packs: List<StickerPack>): Cursor {
        val playStoreLink = AppConfig.playStoreUrl(checkNotNull(context).packageName)
        return MatrixCursor(PACK_COLUMNS).apply {
            packs.forEach { pack ->
                addRow(
                    arrayOf<Any>(
                        pack.id,
                        pack.name,
                        pack.publisher,
                        StickerPackStore.TRAY_FILE,
                        playStoreLink,
                        "", // ios_app_download_link
                        "", // sticker_pack_publisher_email
                        "", // sticker_pack_publisher_website
                        AppConfig.PRIVACY_URL, // sticker_pack_privacy_policy_website
                        "", // sticker_pack_license_agreement_website
                        IMAGE_DATA_VERSION,
                        0, // whatsapp_will_not_cache_stickers
                        0, // animated_sticker_pack
                    ),
                )
            }
        }
    }

    private fun stickersCursor(pack: StickerPack?): Cursor = MatrixCursor(STICKER_COLUMNS).apply {
        pack?.stickerFiles?.forEach { addRow(arrayOf<Any>(it, StickerPackStore.DEFAULT_EMOJI, "")) }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        val segments = uri.pathSegments
        if (matcher.match(uri) != CODE_ASSET) throw FileNotFoundException("Unknown URI: $uri")
        val file = store.assetFile(segments[segments.size - 2], segments.last())
            ?: throw FileNotFoundException("Sticker not found: $uri")
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        CODE_ALL_PACKS -> "vnd.android.cursor.dir/vnd.$authority.metadata"
        CODE_SINGLE_PACK -> "vnd.android.cursor.item/vnd.$authority.metadata"
        CODE_STICKERS -> "vnd.android.cursor.dir/vnd.$authority.stickers"
        CODE_ASSET -> if (uri.lastPathSegment == StickerPackStore.TRAY_FILE) "image/png" else "image/webp"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException("Not supported")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Not supported")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Not supported")

    private companion object {
        const val CODE_ALL_PACKS = 1
        const val CODE_SINGLE_PACK = 2
        const val CODE_STICKERS = 3
        const val CODE_ASSET = 4

        /** File trong gói không đổi sau khi tạo (đổi ảnh thì ra id mới) nên phiên bản luôn là 1. */
        const val IMAGE_DATA_VERSION = "1"

        val PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )

        val STICKER_COLUMNS = arrayOf("sticker_file_name", "sticker_emoji", "sticker_accessibility_text")
    }
}
