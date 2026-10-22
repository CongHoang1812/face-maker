package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface CosplayRepository {
    fun randomChallenge(): Flow<Resource<CosplayChallenge>>
}

class CosplayRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : CosplayRepository {

    override fun randomChallenge() = resourceFlow { remote.getRandomCosplayChallenge() }.flowOn(dispatchers.io)
}
