package com.anchtech.nqueens.common.di

import com.anchtech.nqueens.data.BestTimesDataStore
import com.anchtech.nqueens.domain.repository.BestTimesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindBestTimesRepository(implementation: BestTimesDataStore): BestTimesRepository
}
