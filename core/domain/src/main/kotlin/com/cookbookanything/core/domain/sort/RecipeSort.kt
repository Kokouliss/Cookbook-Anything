package com.cookbookanything.core.domain.sort

import com.cookbookanything.core.domain.model.RecipeSummary
import com.cookbookanything.core.domain.search.searchKey

/** The orders the recipe list offers. [RECENT] is the stored order: last edited first. */
enum class RecipeSort { RECENT, A_TO_Z, COOKED_RECENTLY, NOT_FOR_A_WHILE }

/** Re-orders the list; accents and capitals do not count for A to Z, so "Άρτος" sits under A. Pinned recipes come first in every order. */
fun List<RecipeSummary>.sortedBy(sort: RecipeSort): List<RecipeSummary> = when (sort) {
    RecipeSort.RECENT -> this
    RecipeSort.A_TO_Z -> sortedBy { it.title.searchKey() }
    // Never cooked goes last here and first in NOT_FOR_A_WHILE, so each order says what it promises.
    RecipeSort.COOKED_RECENTLY -> sortedByDescending { it.lastCookedAt ?: Long.MIN_VALUE }
    RecipeSort.NOT_FOR_A_WHILE -> sortedBy { it.lastCookedAt ?: Long.MIN_VALUE }
}.sortedByDescending { it.pinned }
