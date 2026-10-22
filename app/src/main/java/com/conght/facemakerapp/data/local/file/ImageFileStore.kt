package com.conght.facemakerapp.data.local.file

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.conght.facemakerapp.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu ảnh CHỈ trong internal storage của app (`filesDir`).
 * Không dùng MediaStore / external storage, không cần runtime permission.
 */
@Singleton
class ImageFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    private val creationsDir get() = File(context.filesDir, DIR_CREATIONS).apply { mkdirs() }
    private val downloadsDir get() = File(context.filesDir, DIR_DOWNLOADS).apply { mkdirs() }

    /** Lưu PNG vào `filesDir/creations/{uuid}.png`, trả về đường dẫn tuyệt đối. */
    suspend fun saveCreation(bitmap: Bitmap): String = withContext(dispatchers.io) {
        writePng(bitmap, File(creationsDir, "${UUID.randomUUID()}.png"))
    }

    /** Lưu PNG tạm vào `cacheDir/[fileName]` (ghi đè), trả về đường dẫn tuyệt đối. */
    suspend fun saveTemp(bitmap: Bitmap, fileName: String): String = withContext(dispatchers.io) {
        writePng(bitmap, File(context.cacheDir, fileName))
    }

    /**
     * Copy ảnh người dùng chọn (content Uri từ Photo Picker) vào `cacheDir/imports/{uuid}`
     * để dùng lâu dài không phụ thuộc quyền đọc Uri tạm thời. Trả về đường dẫn tuyệt đối.
     */
    suspend fun importImage(uri: Uri): String = withContext(dispatchers.io) {
        val dir = File(context.cacheDir, DIR_IMPORTS).apply { mkdirs() }
        val target = File(dir, UUID.randomUUID().toString())
        val input = requireNotNull(context.contentResolver.openInputStream(uri)) { "Không mở được $uri" }
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        target.absolutePath
    }

    private fun writePng(bitmap: Bitmap, file: File): String {
        file.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Không nén được bitmap" }
        }
        return file.absolutePath
    }

    suspend fun delete(path: String): Boolean = withContext(dispatchers.io) {
        File(path).delete()
    }

    /** Copy sang `filesDir/downloads/DnD_{yyyyMMdd_HHmmss}_{n}.png` (n tăng dần để không trùng). */
    suspend fun copyToDownloads(srcPath: String): File = withContext(dispatchers.io) {
        val src = File(srcPath)
        require(src.exists()) { "Không tìm thấy file: $srcPath" }
        val dir = downloadsDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        var n = 1
        var target: File
        do {
            target = File(dir, "DnD_${timestamp}_$n.png")
            n++
        } while (target.exists())
        src.copyTo(target)
    }

    private companion object {
        const val DIR_CREATIONS = "creations"
        const val DIR_DOWNLOADS = "downloads"
        const val DIR_IMPORTS = "imports"
    }
}
