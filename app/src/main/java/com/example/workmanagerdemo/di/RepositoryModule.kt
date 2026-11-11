package com.example.workmanagerdemo.di

import com.example.workmanagerdemo.data.UserRepository
import com.example.workmanagerdemo.data.UserRepositoryImpl
import com.example.workmanagerdemo.repository.WorkManagerRepository
import com.example.workmanagerdemo.repository.WorkManagerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the domain and WorkManager repository interfaces
 * to their concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkManagerRepository(
        impl: WorkManagerRepositoryImpl
    ): WorkManagerRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
