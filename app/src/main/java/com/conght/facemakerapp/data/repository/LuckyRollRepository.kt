package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface LuckyRollRepository {
    fun roll(): Flow<Resource<LuckyRollResult>>
}

class LuckyRollRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : LuckyRollRepository {

    override fun roll() = resourceFlow { remote.getLuckyRoll() }.flowOn(dispatchers.io)
}
