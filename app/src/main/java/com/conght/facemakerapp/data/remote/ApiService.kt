package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.remote.dto.DecorConfigDto
import com.conght.facemakerapp.data.remote.dto.PartDto
import retrofit2.http.GET

/** Endpoint JSON (GET, không cần header / token). Link ảnh không có trong API mà ghép ở [ApiMapper]. */
interface ApiService {

    /** Toàn bộ template nhân vật: key = template, value = danh sách bộ phận. */
    @GET("api/app/${AppConfig.APP_KEY}")
    suspend fun getAllData(): Map<String, List<PartDto>>

    /** Số lượng background / sticker / speech bubble theo nhóm. */
    @GET("${AppConfig.ASSET_PATH}bg/bg.json")
    suspend fun getDecorConfig(): DecorConfigDto
}
