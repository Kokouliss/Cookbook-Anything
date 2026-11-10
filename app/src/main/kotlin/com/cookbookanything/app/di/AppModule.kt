package com.cookbookanything.app.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cookbookanything.app.data.database.CategoryDao
import com.cookbookanything.app.data.database.CookbookDatabase
import com.cookbookanything.app.data.database.RecipeDao
import com.cookbookanything.app.data.database.ScheduleDao
import com.cookbookanything.app.data.repository.LocalEntitlementRepository
import com.cookbookanything.app.data.repository.OfflineFirstCategoryRepository
import com.cookbookanything.app.data.repository.OfflineFirstRecipeRepository
import com.cookbookanything.app.data.repository.OfflineFirstScheduleRepository
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.repository.EntitlementRepository
import com.cookbookanything.core.domain.repository.RecipeRepository
import com.cookbookanything.core.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CookbookDatabase =
        Room.databaseBuilder<CookbookDatabase>(context = context, name = "cookbook.db")
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    fun provideRecipeDao(database: CookbookDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideCategoryDao(database: CookbookDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideScheduleDao(database: CookbookDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRecipeRepository(impl: OfflineFirstRecipeRepository): RecipeRepository

    @Binds
    abstract fun bindCategoryRepository(impl: OfflineFirstCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindScheduleRepository(impl: OfflineFirstScheduleRepository): ScheduleRepository

    @Binds
    abstract fun bindEntitlementRepository(impl: LocalEntitlementRepository): EntitlementRepository
}
