package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail

interface MakerRemoteDataSource {
    suspend fun getTemplates(): List<CharacterTemplate>
    suspend fun getTemplateDetail(templateId: String): TemplateDetail
    suspend fun getDecorPacks(type: DecorType): List<DecorPack>
    suspend fun getRandomCosplayChallenge(): CosplayChallenge
    suspend fun getLuckyRoll(): LuckyRollResult
    suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection?
    suspend fun randomizeCharacter(templateId: String): CharacterConfig?
}
