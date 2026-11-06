package com.fantasy.oc.maker.customize.data.remote

import com.fantasy.oc.maker.customize.data.model.CharacterConfig
import com.fantasy.oc.maker.customize.data.model.CharacterTemplate
import com.fantasy.oc.maker.customize.data.model.CosplayChallenge
import com.fantasy.oc.maker.customize.data.model.DecorPack
import com.fantasy.oc.maker.customize.data.model.DecorType
import com.fantasy.oc.maker.customize.data.model.LuckyRollResult
import com.fantasy.oc.maker.customize.data.model.PartSelection
import com.fantasy.oc.maker.customize.data.model.TemplateDetail

interface MakerRemoteDataSource {
    suspend fun getTemplates(): List<CharacterTemplate>
    suspend fun getTemplateDetail(templateId: String): TemplateDetail
    suspend fun getDecorPacks(type: DecorType): List<DecorPack>
    suspend fun getRandomCosplayChallenge(): CosplayChallenge
    suspend fun getLuckyRoll(): LuckyRollResult
    suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection?
    suspend fun randomizeCharacter(templateId: String): CharacterConfig?
}
