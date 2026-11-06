package com.conght.facemakerapp.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.galleryAlbumName

/**
 * Bọc thao tác ghi ảnh ra Gallery: Android 10+ chạy luôn (MediaStore không cần quyền),
 * Android 9 trở xuống xin WRITE_EXTERNAL_STORAGE trước.
 * Gọi lúc khởi tạo Fragment (khai báo property) vì có đăng ký ActivityResult.
 */
fun Fragment.registerGalleryDownload(download: () -> Unit): () -> Unit {
    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) download() else showAppToast(R.string.save_permission_denied)
    }
    return {
        val canWrite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        if (canWrite) download() else launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

/** Toast sau khi Download ra Gallery thành công, kèm tên album `Pictures/{tên app}`. */
fun Fragment.showDownloadSuccessToast() {
    val anchor = view ?: return
    anchor.showAppToast(getString(R.string.download_success, anchor.context.galleryAlbumName()))
}
