package com.example.workmanagerdemo.di

import com.example.workmanagerdemo.repository.WorkManagerRepository
import com.example.workmanagerdemo.repository.WorkManagerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [WorkManagerRepository] interface
 * to its concrete implementation [WorkManagerRepositoryImpl].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkManagerRepository(
        impl: WorkManagerRepositoryImpl
    ): WorkManagerRepository
}
