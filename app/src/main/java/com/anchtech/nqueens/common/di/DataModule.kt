package com.anchtech.nqueens.common.di

import com.anchtech.nqueens.data.SettingsDataStore
import com.anchtech.nqueens.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindSettingsRepository(implementation: SettingsDataStore): SettingsRepository
}
