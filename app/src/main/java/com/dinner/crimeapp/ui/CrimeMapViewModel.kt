package com.dinner.crimeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.CrimeRepository
import com.dinner.crimeapp.data.OutcomeEntry
import com.dinner.crimeapp.data.GeocodeResult
import com.dinner.crimeapp.data.StopSearch
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.FlowPreview
import kotlin.time.Duration.Companion.milliseconds

enum class ViewMode { CRIMES, STOP_SEARCH }

data class CrimeMapState(
    val isLoading: Boolean = false,
    val crimesByMonth: Map<String, List<Crime>> = emptyMap(),
    val stopSearchesByMonth: Map<String, List<StopSearch>> = emptyMap(),
    val crimesRateLimited: Boolean = false,
    val stopsRateLimited: Boolean = false,
    val viewMode: ViewMode = ViewMode.CRIMES,
    val selectedMonth: String? = null, // null = show all fetched months
    val categories: List<com.dinner.crimeapp.data.CrimeCategory> = emptyList(),
    val selectedCategory: String? = null, // category.url value, null = all
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<GeocodeResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

data class StopSearchSummary(
    val total: Int,
    val mostCommonType: String?,
    val mostCommonOutcome: String?
)

data class CrimeSummary(
    val totalCrimes: Int,
    val mostCommonCategory: String?,
    val mostCommonCategoryCount: Int,
    val resolvedCount: Int,
    val underInvestigationCount: Int
)

data class CategoryCount(val category: String, val count: Int)

@OptIn(FlowPreview::class)
class CrimeMapViewModel(
    private val repository: CrimeRepository = CrimeRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CrimeMapState())
    val state: StateFlow<CrimeMapState> = _state.asStateFlow()

    private val _outcomeHistory = MutableStateFlow<List<OutcomeEntry>>(emptyList())
    val outcomeHistory: StateFlow<List<OutcomeEntry>> = _outcomeHistory.asStateFlow()

    private val _outcomeLoading = MutableStateFlow(false)
    val outcomeLoading: StateFlow<Boolean> = _outcomeLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodeResult>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")

    init {
        loadCategories()
        setupSearchDebounce()
    }

    private fun setupSearchDebounce() {
        _searchQueryFlow
            .debounce(1000.milliseconds)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach { query ->
                searchPlace(query)
            }
            .launchIn(viewModelScope)
    }

    fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.getCategories() }
                .onSuccess { cats -> _state.value = _state.value.copy(categories = cats) }
            // fail silently here — categories are a nice-to-have filter, not critical path
        }
    }

    fun loadCrimes(lat: Double, lng: Double, monthsBack: Int = 6) {
        Log.d("CrimeMapViewModel", "loadCrimes: $lat, $lng")
        viewModelScope.launch {
            // Clear previous data immediately to avoid showing "ghost" markers from old locations
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                crimesByMonth = emptyMap(),
                stopSearchesByMonth = emptyMap(),
                crimesRateLimited = false,
                stopsRateLimited = false
            )
            
            val crimesDeferred = async { 
                runCatching { repository.getCrimesForRange(lat, lng, monthsBack) }
            }
            val stopsDeferred = async {
                runCatching { repository.getStopSearchesForRange(lat, lng, monthsBack) }
            }

            val crimesResult = crimesDeferred.await()
            val stopsResult = stopsDeferred.await()

            val (crimesData, crimesLimited, crimesError) = crimesResult.getOrDefault(Triple(emptyMap(), false, null))
            val (stopsData, stopsLimited, stopsError) = stopsResult.getOrDefault(Triple(emptyMap(), false, null))

            _state.value = _state.value.copy(
                isLoading = false,
                crimesByMonth = crimesData,
                stopSearchesByMonth = stopsData,
                crimesRateLimited = crimesLimited,
                stopsRateLimited = stopsLimited,
                error = crimesError ?: stopsError ?: crimesResult.exceptionOrNull()?.message ?: stopsResult.exceptionOrNull()?.message
            )
        }
    }

    fun setViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
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

    /** Stop searches visible given the current filters. */
    fun visibleStopSearches(): List<StopSearch> {
        val state = _state.value
        val byMonth = if (state.selectedMonth == null) {
            state.stopSearchesByMonth.values.flatten()
        } else {
            state.stopSearchesByMonth[state.selectedMonth].orEmpty()
        }
        // No category filter for stop searches currently
        return byMonth
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

    fun stopSearchSummary(): StopSearchSummary {
        val stops = visibleStopSearches()
        val topType = stops.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
        val topOutcome = stops.groupingBy { it.outcome }.eachCount().maxByOrNull { it.value }?.key
        
        return StopSearchSummary(
            total = stops.size,
            mostCommonType = topType,
            mostCommonOutcome = topOutcome
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

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query, searchError = null)
        _searchQueryFlow.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    fun searchPlace(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searching.value = true
            _state.value = _state.value.copy(searchError = null)
            val results = repository.searchPlace(query)
            _searchResults.value = results
            // Also sync state for legacy UI until fully migrated
            _state.value = _state.value.copy(
                isSearching = false,
                searchResults = results
            )
            _searching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun performSearch(query: String = _state.value.searchQuery) {
        searchPlace(query)
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", searchResults = emptyList())
    }
}
