package com.conght.facemakerapp.data.model

data class CosplayChallenge(val id: String, val referenceImageUrl: String, val templateId: String, val answer: CharacterConfig)

data class LuckyRollResult(val imageUrl: String, val config: CharacterConfig)
