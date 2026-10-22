package com.conght.facemakerapp.data.model

enum class DecorType { BACKGROUND, STICKER, SPEECH }

data class DecorPack(val id: String, val name: String, val type: DecorType, val items: List<DecorItem>)

data class DecorItem(val id: String, val thumbnailUrl: String, val imageUrl: String)
