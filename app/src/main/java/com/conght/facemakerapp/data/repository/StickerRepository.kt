package com.conght.facemakerapp.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.data.local.file.StickerPackStore
import com.conght.facemakerapp.data.local.file.stickerPackId
import com.conght.facemakerapp.data.model.StickerExport
import com.conght.facemakerapp.data.model.StickerTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface StickerRepository {
    /**
     * Tạo sticker từ [imagePaths] và cho biết bước tiếp theo với app đích.
     * [packName]: tên gói người dùng đặt (WhatsApp); `null` = tên mặc định.
     */
    suspend fun export(target: StickerTarget, imagePaths: List<String>, packName: String?): Result<StickerExport>
}

class StickerRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: StickerPackStore,
    private val dispatchers: DispatcherProvider,
) : StickerRepository {

    override suspend fun export(target: StickerTarget, imagePaths: List<String>, packName: String?): Result<StickerExport> =
        withContext(dispatchers.io) {
            try {
                Result.success(
                    when (target) {
                        StickerTarget.WHATSAPP -> exportToWhatsApp(imagePaths, packName)
                        StickerTarget.TELEGRAM -> StickerExport.Telegram(
                            paths = store.writeTelegramStickers(imagePaths),
                            emojis = imagePaths.map { StickerPackStore.DEFAULT_EMOJI },
                        )
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun exportToWhatsApp(imagePaths: List<String>, packName: String?): StickerExport {
        val installed = WHATSAPP_PACKAGES.filter(::isInstalled)
        if (installed.isEmpty()) return StickerExport.WhatsAppNotInstalled
        val packId = stickerPackId(imagePaths)
        val missing = installed.filterNot { isPackAdded(it, packId) }
        if (missing.isEmpty()) return StickerExport.WhatsAppAlreadyAdded
        val pack = store.getOrCreatePack(imagePaths, packName)
        return StickerExport.WhatsApp(pack.id, pack.name, missing)
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** WhatsApp cũ không có provider này hoặc truy vấn lỗi: coi như chưa thêm để vẫn gửi yêu cầu được. */
    private fun isPackAdded(whatsAppPackage: String, packId: String): Boolean = runCatching {
        val authority = "$whatsAppPackage.provider.sticker_whitelist_check"
        if (context.packageManager.resolveContentProvider(authority, PackageManager.GET_META_DATA) == null) {
            return@runCatching false
        }
        val uri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("is_whitelisted")
            .appendQueryParameter("authority", StickerPackStore.authority(context.packageName))
            .appendQueryParameter("identifier", packId)
            .build()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow("result")) == 1
        } ?: false
    }.getOrDefault(false)

    private companion object {
        val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
