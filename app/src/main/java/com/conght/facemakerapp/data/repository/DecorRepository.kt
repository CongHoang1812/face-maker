package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface DecorRepository {
    fun packs(type: DecorType): Flow<Resource<List<DecorPack>>>
}

class DecorRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : DecorRepository {

    override fun packs(type: DecorType) = resourceFlow { remote.getDecorPacks(type) }.flowOn(dispatchers.io)
}
