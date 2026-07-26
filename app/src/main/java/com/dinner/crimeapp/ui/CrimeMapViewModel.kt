package com.dinner.crimeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.CrimeRepository
import com.dinner.crimeapp.data.OutcomeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrimeMapState(
    val isLoading: Boolean = false,
    val crimesByMonth: Map<String, List<Crime>> = emptyMap(),
    val selectedMonth: String? = null, // null = show all fetched months
    val categories: List<com.dinner.crimeapp.data.CrimeCategory> = emptyList(),
    val selectedCategory: String? = null, // category.url value, null = all
    val error: String? = null
)

data class CrimeSummary(
    val totalCrimes: Int,
    val mostCommonCategory: String?,
    val mostCommonCategoryCount: Int,
    val resolvedCount: Int,
    val underInvestigationCount: Int
)

data class CategoryCount(val category: String, val count: Int)

class CrimeMapViewModel(
    private val repository: CrimeRepository = CrimeRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CrimeMapState())
    val state: StateFlow<CrimeMapState> = _state.asStateFlow()

    private val _outcomeHistory = MutableStateFlow<List<OutcomeEntry>>(emptyList())
    val outcomeHistory: StateFlow<List<OutcomeEntry>> = _outcomeHistory.asStateFlow()

    private val _outcomeLoading = MutableStateFlow(false)
    val outcomeLoading: StateFlow<Boolean> = _outcomeLoading.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.getCategories() }
                .onSuccess { cats -> _state.value = _state.value.copy(categories = cats) }
            // fail silently here — categories are a nice-to-have filter, not critical path
        }
    }

    fun loadCrimes(lat: Double, lng: Double, monthsBack: Int = 6) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { repository.getCrimesForRange(lat, lng, monthsBack) }
                .onSuccess { data ->
                    _state.value = _state.value.copy(isLoading = false, crimesByMonth = data)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun selectMonth(month: String?) {
        _state.value = _state.value.copy(selectedMonth = month)
    }

    fun selectCategory(categoryUrl: String?) {
        _state.value = _state.value.copy(selectedCategory = categoryUrl)
    }

    /** Crimes currently visible on the map/list, given the month filter. */
    fun visibleCrimes(): List<Crime> {
        val state = _state.value
        val byMonth = if (state.selectedMonth == null) {
            state.crimesByMonth.values.flatten()
        } else {
            state.crimesByMonth[state.selectedMonth].orEmpty()
        }
        return if (state.selectedCategory == null) {
            byMonth
        } else {
            byMonth.filter { it.category == state.selectedCategory }
        }
    }

    fun summary(): CrimeSummary {
        val crimes = visibleCrimes()
        val byCategory = crimes.groupingBy { it.category }.eachCount()
        val top = byCategory.maxByOrNull { it.value }

        val resolved = crimes.count { it.outcomeStatus != null }
        val unresolved = crimes.size - resolved

        return CrimeSummary(
            totalCrimes = crimes.size,
            mostCommonCategory = top?.key?.replace("-", " "),
            mostCommonCategoryCount = top?.value ?: 0,
            resolvedCount = resolved,
            underInvestigationCount = unresolved
        )
    }

    fun categoryBreakdown(): List<CategoryCount> {
        return visibleCrimes()
            .groupingBy { it.category }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { CategoryCount(it.key, it.value) }
    }

    fun getDominantTheme(): IconSwitcher.CrimeTheme {
        val crimes = visibleCrimes()
        if (crimes.isEmpty()) return IconSwitcher.CrimeTheme.DEFAULT
        
        val topCategory = crimes.groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: return IconSwitcher.CrimeTheme.DEFAULT

        return when {
            topCategory.contains("theft", ignoreCase = true) || 
            topCategory.contains("burglary", ignoreCase = true) -> IconSwitcher.CrimeTheme.THEFT
            topCategory.contains("violence", ignoreCase = true) || 
            topCategory.contains("weapon", ignoreCase = true) -> IconSwitcher.CrimeTheme.VIOLENCE
            else -> IconSwitcher.CrimeTheme.DEFAULT
        }
    }

    fun loadOutcomeHistory(persistentId: String) {
        if (persistentId.isBlank()) {
            _outcomeHistory.value = emptyList()
            return
        }
        viewModelScope.launch {
            _outcomeLoading.value = true
            _outcomeHistory.value = repository.getOutcomeHistory(persistentId)
            _outcomeLoading.value = false
        }
    }

    fun clearOutcomeHistory() {
        _outcomeHistory.value = emptyList()
    }
}
