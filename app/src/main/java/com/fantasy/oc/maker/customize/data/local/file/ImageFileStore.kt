package com.fantasy.oc.maker.customize.data.local.file

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.fantasy.oc.maker.customize.core.common.DispatcherProvider
import com.fantasy.oc.maker.customize.core.common.galleryAlbumName
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
 * Ảnh của app nằm trong internal storage (`filesDir`, `cacheDir`).
 * Chỉ [copyToGallery] (nút Download) mới ghi ra thư viện máy.
 */
@Singleton
class ImageFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    private val creationsDir get() = File(context.filesDir, DIR_CREATIONS).apply { mkdirs() }

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

    /** Copy ra thư viện máy, album `Pictures/{tên app}`, tên file `DnD_{yyyyMMdd_HHmmss}.png`. */
    suspend fun copyToGallery(srcPath: String) = withContext(dispatchers.io) {
        val src = File(srcPath)
        require(src.exists()) { "Không tìm thấy file: $srcPath" }
        val baseName = "DnD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyToMediaStore(src, baseName)
        } else {
            copyToPublicPictures(src, baseName)
        }
    }

    /** IS_PENDING để Gallery không thấy ảnh ghi dở; trùng tên thì MediaStore tự đổi tên. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyToMediaStore(src: File, baseName: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.png")
            put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/${context.galleryAlbumName()}")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values)) { "Không tạo được ảnh trong MediaStore" }
        try {
            val output = checkNotNull(resolver.openOutputStream(uri)) { "Không mở được $uri" }
            output.use { out -> src.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /** Android 9 trở xuống: cần quyền WRITE_EXTERNAL_STORAGE, xem `registerGalleryDownload`. */
    @Suppress("DEPRECATION")
    private fun copyToPublicPictures(src: File, baseName: String) {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(pictures, context.galleryAlbumName())
        check(dir.isDirectory || dir.mkdirs()) { "Không tạo được thư mục: $dir" }
        var target = File(dir, "$baseName.png")
        var n = 1
        while (target.exists()) {
            target = File(dir, "${baseName}_$n.png")
            n++
        }
        src.copyTo(target)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(MIME_PNG), null)
    }

    private companion object {
        const val DIR_CREATIONS = "creations"
        const val DIR_IMPORTS = "imports"
        const val MIME_PNG = "image/png"
    }
}
