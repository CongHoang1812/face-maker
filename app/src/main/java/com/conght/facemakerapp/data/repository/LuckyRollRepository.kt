package com.conght.facemakerapp.data.repository

import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.core.common.Prepared
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.resourceFlow
import com.conght.facemakerapp.data.model.LuckyRollResult
import com.conght.facemakerapp.data.remote.MakerRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface LuckyRollRepository {
    fun roll(): Flow<Resource<LuckyRollResult>>

    /** Bắt đầu tính trước Lucky roll kế tiếp ở nền (gọi khi vào màn) để lần random gần như có ngay. */
    fun prepare() {}
}

/** Singleton: kết quả tính trước sống qua các lần vào màn. */
@Singleton
class LuckyRollRepositoryImpl @Inject constructor(
    private val remote: MakerRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : LuckyRollRepository {

    private val prepared = Prepared(CoroutineScope(SupervisorJob() + dispatchers.io)) { remote.getLuckyRoll() }

    override fun prepare() = prepared.prepare()

    override fun roll() = resourceFlow { prepared.take() }.flowOn(dispatchers.io)
}
