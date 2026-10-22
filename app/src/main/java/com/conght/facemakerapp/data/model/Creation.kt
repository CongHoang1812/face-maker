package com.conght.facemakerapp.data.model

enum class CreationType { CHARACTER, DESIGN }

data class Creation(val id: Long, val type: CreationType, val imagePath: String, val configJson: String?, val createdAt: Long)
