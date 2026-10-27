package com.conght.facemakerapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Một bộ phận của template (endpoint `api/app/{APP_KEY}`, response = Map<templateKey, List<PartDto>>).
 * Field nullable vì Gson không đảm bảo non-null với Kotlin.
 */
data class PartDto(
    /** Tên thư mục template trên server. */
    @SerializedName("position") val position: String?,
    /** `X-Y`: X = zIndex (lớp vẽ), Y = thứ tự tab (tài liệu API ghi ngược, đã kiểm tra bằng ảnh thật). */
    @SerializedName("parts") val parts: String?,
    /** Mã màu cách nhau bởi dấu phẩy; rỗng = không có biến thể màu. */
    @SerializedName("colorArray") val colorArray: String?,
    @SerializedName("quantity") val quantity: Int?,
    /** Thứ tự sắp xếp template. */
    @SerializedName("level") val level: Int?,
)

/** Cấu hình nền / sticker / speech bubble (`bg/bg.json`). */
data class DecorConfigDto(
    @SerializedName("background") val background: List<DecorCategoryDto>?,
    @SerializedName("sticker") val sticker: List<DecorCategoryDto>?,
    @SerializedName("speech bubble") val speechBubble: List<DecorCategoryDto>?,
)

/** [category] = tên thư mục con (có thể rỗng), [quantity] = số ảnh đánh số từ 1. */
data class DecorCategoryDto(
    @SerializedName("category") val category: String?,
    @SerializedName("quantity") val quantity: Int?,
)
