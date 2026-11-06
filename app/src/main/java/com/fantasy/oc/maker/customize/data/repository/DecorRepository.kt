package com.fantasy.oc.maker.customize.data.repository

import com.fantasy.oc.maker.customize.core.common.DispatcherProvider
import com.fantasy.oc.maker.customize.core.common.Resource
import com.fantasy.oc.maker.customize.core.common.resourceFlow
import com.fantasy.oc.maker.customize.data.model.DecorPack
import com.fantasy.oc.maker.customize.data.model.DecorType
import com.fantasy.oc.maker.customize.data.remote.MakerRemoteDataSource
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
