package com.caracolas.gestortrainer.di

import android.content.Context
import com.caracolas.gestortrainer.BuildConfig
import com.caracolas.gestortrainer.core.auth.InMemorySessionRepository
import com.caracolas.gestortrainer.core.auth.SessionRepository
import com.caracolas.gestortrainer.core.database.AppDatabase
import com.caracolas.gestortrainer.core.database.DatabaseFactory
import com.caracolas.gestortrainer.core.database.SyncQueueDao
import com.caracolas.gestortrainer.core.events.EventBus
import com.caracolas.gestortrainer.core.events.InMemoryEventBus
import com.caracolas.gestortrainer.core.featureflags.FeatureFlags
import com.caracolas.gestortrainer.core.featureflags.StaticFeatureFlags
import com.caracolas.gestortrainer.core.network.ApiConfig
import com.caracolas.gestortrainer.core.network.AuthTokenProvider
import com.caracolas.gestortrainer.core.network.RetrofitFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** MVP: todo lo reservado apagado (nutrición, grupales, 3D, AR, premios, compartir). */
    @Provides
    @Singleton
    fun provideFeatureFlags(): FeatureFlags = StaticFeatureFlags()

    @Provides
    @Singleton
    fun provideEventBus(): EventBus = InMemoryEventBus()

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(baseUrl = "https://api.gestortrainer.caracolasgames.com/")

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = InMemorySessionRepository()

    /** TODO: el Session actual no lleva token todavía; llegará con el backend de Cognito. */
    @Provides
    @Singleton
    fun provideAuthTokenProvider(): AuthTokenProvider = AuthTokenProvider { null }

    @Provides
    @Singleton
    fun provideRetrofit(apiConfig: ApiConfig, authTokenProvider: AuthTokenProvider): Retrofit =
        RetrofitFactory(apiConfig, authTokenProvider, debug = BuildConfig.DEBUG).create()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        DatabaseFactory.create(context)

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()
}
