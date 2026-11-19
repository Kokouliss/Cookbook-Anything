package com.cookbookanything.app.data.repository

import com.cookbookanything.app.data.database.CategoryDao
import com.cookbookanything.app.data.database.CategoryEntity
import com.cookbookanything.app.data.database.toModel
import com.cookbookanything.core.domain.model.Category
import com.cookbookanything.core.domain.repository.CategoryRepository
import com.cookbookanything.core.domain.search.searchKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineFirstCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    // Sorted here because SQLite's NOCASE only understands ASCII, which would misplace Greek names;
    // accents do not count either, so "Άρτος" sits under A.
    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategories().map { rows ->
            rows.map(CategoryEntity::toModel).sortedBy { it.name.searchKey() }
        }

    /** "Γλυκά" and "γλυκα" are the same category: the existing one comes back instead of a twin. */
    override suspend fun addCategory(name: String): Category {
        sameNamed(name)?.let { return it.toModel() }
        val category = Category(id = UUID.randomUUID().toString(), name = name.trim())
        categoryDao.upsert(CategoryEntity(id = category.id, name = category.name))
        return category
    }

    override suspend fun renameCategory(id: String, name: String) {
        val twin = sameNamed(name)
        if (twin != null && twin.id != id) return
        categoryDao.upsert(CategoryEntity(id = id, name = name.trim()))
    }

    private suspend fun sameNamed(name: String): CategoryEntity? {
        val key = name.trim().searchKey()
        return categoryDao.getAll().firstOrNull { it.name.searchKey() == key }
    }

    // Recipes in the category are kept; the foreign key clears their category.
    override suspend fun deleteCategory(id: String) = categoryDao.delete(id)
}
