package com.conght.facemakerapp.data.local.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.core.graphics.createBitmap
import com.conght.facemakerapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val sequence: Int,
    val stickerFiles: List<String>,
)

/**
 * Sticker tạo từ ảnh nhân vật, nằm trong internal storage:
 * - WhatsApp: `filesDir/sticker_packs/{id}/`, giữ lâu dài vì WhatsApp đọc lại qua [StickerContentProvider].
 * - Telegram: `cacheDir/telegram_stickers/`, chỉ cần tới khi Telegram import xong.
 *
 * Hàm ở đây chặn luồng, gọi từ IO dispatcher.
 */
@Singleton
class StickerPackStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val packsDir get() = File(context.filesDir, DIR_PACKS)

    /** Cùng danh sách ảnh thì trả lại gói cũ, nên WhatsApp nhận ra gói đã thêm. */
    @Synchronized
    fun getOrCreatePack(imagePaths: List<String>): StickerPack {
        val id = stickerPackId(imagePaths)
        pack(id)?.let { return it }

        val existing = packs()
        val appName = context.getString(R.string.app_name)
        val sequence = (existing.maxOfOrNull { it.sequence } ?: 0) + 1
        val pack = StickerPack(
            id = id,
            name = "$appName $sequence",
            publisher = appName,
            sequence = sequence,
            stickerFiles = imagePaths.indices.map(::stickerFileName),
        )

        // Ghi vào thư mục tạm rồi đổi tên để provider không bao giờ thấy gói ghi dở
        val temp = File(packsDir, "$TEMP_PREFIX$id").apply { deleteRecursively() }
        check(temp.mkdirs()) { "Không tạo được thư mục: $temp" }
        try {
            imagePaths.forEachIndexed { index, path ->
                decodeSticker(path).useBitmap { sticker ->
                    File(temp, pack.stickerFiles[index]).writeBytes(encodeWebp(sticker))
                    if (index == 0) File(temp, TRAY_FILE).writeBytes(encodeTray(sticker))
                }
            }
            File(temp, INFO_FILE).writeText(pack.toJson())
            check(temp.renameTo(File(packsDir, id))) { "Không đổi tên được: $temp" }
        } catch (e: Exception) {
            temp.deleteRecursively()
            throw e
        }

        // WhatsApp cho mỗi app tối đa 10 gói: bỏ gói cũ nhất
        (existing + pack).sortedByDescending { it.sequence }.drop(MAX_PACKS).forEach {
            File(packsDir, it.id).deleteRecursively()
        }
        return pack
    }

    fun packs(): List<StickerPack> =
        packsDir.list().orEmpty().mapNotNull(::pack).sortedBy { it.sequence }

    fun pack(id: String): StickerPack? {
        if (!PACK_ID.matches(id)) return null
        val info = File(File(packsDir, id), INFO_FILE)
        if (!info.isFile) return null
        return runCatching { parsePack(id, info.readText()) }.getOrNull()
    }

    /** Chỉ trả về file thuộc gói; tên lấy từ Uri bên ngoài nên không ghép thẳng thành đường dẫn. */
    fun assetFile(packId: String, fileName: String): File? {
        val pack = pack(packId) ?: return null
        if (fileName != TRAY_FILE && fileName !in pack.stickerFiles) return null
        return File(File(packsDir, packId), fileName).takeIf { it.isFile }
    }

    /** Ghi đè lần xuất trước, trả về đường dẫn các sticker cho Telegram. */
    fun writeTelegramStickers(imagePaths: List<String>): List<String> {
        val dir = File(context.cacheDir, DIR_TELEGRAM).apply { deleteRecursively() }
        check(dir.mkdirs()) { "Không tạo được thư mục: $dir" }
        return imagePaths.mapIndexed { index, path ->
            val file = File(dir, stickerFileName(index))
            decodeSticker(path).useBitmap { file.writeBytes(encodeWebp(it)) }
            file.absolutePath
        }
    }

    private fun decodeSticker(path: String): Bitmap {
        val source = checkNotNull(BitmapFactory.decodeFile(path)) { "Không đọc được ảnh: $path" }
        return source.useBitmap { fitInSquare(it, STICKER_SIZE) }
    }

    /** Giữ tỉ lệ ảnh, phần thừa trong suốt. */
    private fun fitInSquare(source: Bitmap, size: Int): Bitmap {
        val target = createBitmap(size, size)
        val matrix = Matrix().apply {
            setRectToRect(
                RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
                RectF(0f, 0f, size.toFloat(), size.toFloat()),
                Matrix.ScaleToFit.CENTER,
            )
        }
        Canvas(target).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return target
    }

    private fun encodeWebp(sticker: Bitmap): ByteArray =
        compressWithinLimit(MAX_STICKER_BYTES) { quality -> sticker.compressToBytes(webpFormat(), quality) }

    private fun encodeTray(sticker: Bitmap): ByteArray =
        fitInSquare(sticker, TRAY_SIZE).useBitmap { it.compressToBytes(Bitmap.CompressFormat.PNG, 100) }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

    private fun Bitmap.compressToBytes(format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        check(compress(format, quality, out)) { "Không nén được bitmap" }
        return out.toByteArray()
    }

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T = try {
        block(this)
    } finally {
        recycle()
    }

    private fun StickerPack.toJson(): String = JSONObject()
        .put(KEY_NAME, name)
        .put(KEY_PUBLISHER, publisher)
        .put(KEY_SEQUENCE, sequence)
        .put(KEY_STICKERS, JSONArray(stickerFiles))
        .toString()

    private fun parsePack(id: String, json: String): StickerPack {
        val root = JSONObject(json)
        val files = root.getJSONArray(KEY_STICKERS)
        return StickerPack(
            id = id,
            name = root.getString(KEY_NAME),
            publisher = root.getString(KEY_PUBLISHER),
            sequence = root.getInt(KEY_SEQUENCE),
            stickerFiles = List(files.length(), files::getString),
        )
    }

    companion object {
        const val TRAY_FILE = "tray.png"

        /** Emoji gắn cho mọi sticker: WhatsApp và Telegram đều bắt buộc có, app chưa cho người dùng chọn. */
        const val DEFAULT_EMOJI = "😀"

        fun authority(packageName: String) = "$packageName.stickercontentprovider"

        private const val DIR_PACKS = "sticker_packs"
        private const val DIR_TELEGRAM = "telegram_stickers"
        private const val INFO_FILE = "pack.json"
        private const val TEMP_PREFIX = "tmp_"
        private const val MAX_PACKS = 10
        private const val STICKER_SIZE = 512
        private const val TRAY_SIZE = 96
        private const val MAX_STICKER_BYTES = 100 * 1024
        private const val KEY_NAME = "name"
        private const val KEY_PUBLISHER = "publisher"
        private const val KEY_SEQUENCE = "sequence"
        private const val KEY_STICKERS = "stickers"
        private val PACK_ID = Regex("pack_[0-9a-f]+")

        private fun stickerFileName(index: Int) = "%02d.webp".format(java.util.Locale.US, index + 1)
    }
}

/** Id ổn định theo danh sách ảnh; đường dẫn ảnh là UUID nên không trùng giữa các nhân vật. */
internal fun stickerPackId(imagePaths: List<String>): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(imagePaths.joinToString("\n").toByteArray())
    return "pack_" + digest.take(16).joinToString("") { "%02x".format(java.util.Locale.US, it) }
}

/** Giảm dần chất lượng tới khi ảnh không vượt [limitBytes]. */
internal fun compressWithinLimit(limitBytes: Int, compress: (quality: Int) -> ByteArray): ByteArray {
    for (quality in 90 downTo 10 step 10) {
        val bytes = compress(quality)
        if (bytes.size <= limitBytes) return bytes
    }
    error("Không nén được ảnh xuống dưới $limitBytes byte")
}
