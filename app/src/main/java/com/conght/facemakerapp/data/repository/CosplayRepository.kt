package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.core.common.Prepared
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface CosplayRepository {
    fun randomChallenge(): Flow<Resource<CosplayChallenge>>

    /** Bắt đầu tính trước đề Cosplay kế tiếp ở nền (gọi khi vào màn) để lần random gần như có ngay. */
    fun prepare() {}
}

/** Singleton: kết quả tính trước sống qua các lần vào màn. */
@Singleton
class CosplayRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : CosplayRepository {

    private val prepared = Prepared(CoroutineScope(SupervisorJob() + dispatchers.io)) { remote.getRandomCosplayChallenge() }

    override fun prepare() = prepared.prepare()

    override fun randomChallenge() = resourceFlow { prepared.take() }.flowOn(dispatchers.io)
}
