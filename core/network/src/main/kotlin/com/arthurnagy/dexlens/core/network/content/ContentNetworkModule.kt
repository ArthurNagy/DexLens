package com.arthurnagy.dexlens.core.network.content

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ContentNetworkModule {
    @Binds
    fun bindsContentRemoteDataSource(impl: RetrofitContentRemoteDataSource): ContentRemoteDataSource
}
