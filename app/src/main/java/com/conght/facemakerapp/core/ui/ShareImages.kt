package com.conght.facemakerapp.core.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Chia sẻ ảnh trong internal storage (filesDir/creations, filesDir/downloads, cacheDir)
 * qua FileProvider `${applicationId}.fileprovider` – không cần quyền storage.
 * Trả về false nếu không có app nào nhận / file không chia sẻ được.
 */
fun Context.shareImages(paths: List<String>, chooserTitle: CharSequence): Boolean {
    if (paths.isEmpty() || paths.any { !File(it).isFile }) return false
    val uris: List<Uri> = try {
        paths.map { FileProvider.getUriForFile(this, "$packageName.fileprovider", File(it)) }
    } catch (_: IllegalArgumentException) {
        return false
    }
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }.apply {
        type = MIME_PNG
        // ClipData để quyền đọc được cấp cho mọi Uri (kể cả qua chooser)
        clipData = ClipData.newUri(contentResolver, null, uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private const val MIME_PNG = "image/png"
