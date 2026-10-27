package com.conght.facemakerapp

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.conght.facemakerapp.data.local.db.SampleCreationSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class FaceMakerApp : Application() {

    @Inject lateinit var sampleCreationSeeder: SampleCreationSeeder

    @Inject lateinit var okHttpClient: OkHttpClient

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // App không có dark theme -> khoá light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        appScope.launch { sampleCreationSeeder.seedIfNeeded() }
        resetConnectionsOnNetworkChange()
    }

    /**
     * Đổi mạng (vd. 4G → Wi-Fi): đóng kết nối cũ trong pool OkHttp (API + Glide dùng chung), nếu không
     * request vẫn đi qua mạng cũ – nơi server có thể đang chặn ảnh.
     */
    private fun resetConnectionsOnNetworkChange() {
        val manager = getSystemService<ConnectivityManager>() ?: return
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            private var current: Network? = null

            override fun onAvailable(network: Network) {
                if (current != null && current != network) okHttpClient.connectionPool.evictAll()
                current = network
            }
        })
    }
}
