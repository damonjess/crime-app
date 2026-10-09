package com.dinner.crimeapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinner.crimeapp.data.AreaInfo
import com.dinner.crimeapp.data.Crime
import com.dinner.crimeapp.data.CrimeRepository
import com.dinner.crimeapp.data.OutcomeEntry
import com.dinner.crimeapp.data.GeocodeResult
import com.dinner.crimeapp.data.StopSearch
import com.dinner.crimeapp.data.AppDatabase
import com.dinner.crimeapp.data.AccidentDataInitializer
import com.dinner.crimeapp.data.RoadAccidentEntity
import org.osmdroid.views.MapView
import android.content.Context
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
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.milliseconds

enum class ViewMode { CRIMES, STOP_SEARCH, ACCIDENTS }

data class CrimeMapState(
    val isLoading: Boolean = false,
    val crimesByMonth: Map<String, List<Crime>> = emptyMap(),
    val stopSearchesByMonth: Map<String, List<StopSearch>> = emptyMap(),
    val visibleAccidents: List<RoadAccidentEntity> = emptyList(),
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
class CrimeMapViewModel(application: Application) : AndroidViewModel(application) {

    // Property initializer (not a constructor param) so the default
    // AndroidViewModelFactory can instantiate this class via reflection.
    private val repository: CrimeRepository = CrimeRepository(application)

    private val appContext: Context = application

    private val _state = MutableStateFlow(CrimeMapState())
    val state: StateFlow<CrimeMapState> = _state.asStateFlow()

    private val _outcomeHistory = MutableStateFlow<List<OutcomeEntry>>(emptyList())
    val outcomeHistory: StateFlow<List<OutcomeEntry>> = _outcomeHistory.asStateFlow()

    private val _outcomeLoading = MutableStateFlow(false)
    val outcomeLoading: StateFlow<Boolean> = _outcomeLoading.asStateFlow()

    private val _areaProfile = MutableStateFlow<com.dinner.crimeapp.data.PostcodeResult?>(null)
    val areaProfile: StateFlow<com.dinner.crimeapp.data.PostcodeResult?> = _areaProfile.asStateFlow()

    private val _areaProfileLoading = MutableStateFlow(false)
    val areaProfileLoading: StateFlow<Boolean> = _areaProfileLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodeResult>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")

    // ---------- Area / neighbourhood state ----------
    private val _isAreaLoading = MutableStateFlow(false)
    val isAreaLoading: StateFlow<Boolean> = _isAreaLoading.asStateFlow()

    private val _areaError = MutableStateFlow<String?>(null)
    val areaError: StateFlow<String?> = _areaError.asStateFlow()

    private val _areaInfo = MutableStateFlow<AreaInfo?>(null)
    val areaInfo: StateFlow<AreaInfo?> = _areaInfo.asStateFlow()

    private val _areaLocationName = MutableStateFlow<String?>(null)
    val areaLocationName: StateFlow<String?> = _areaLocationName.asStateFlow()

    private val _team = MutableStateFlow<List<com.dinner.crimeapp.data.NeighbourhoodOfficer>>(emptyList())
    val team: StateFlow<List<com.dinner.crimeapp.data.NeighbourhoodOfficer>> = _team.asStateFlow()

    private val _events = MutableStateFlow<List<com.dinner.crimeapp.data.NeighbourhoodEvent>>(emptyList())
    val events: StateFlow<List<com.dinner.crimeapp.data.NeighbourhoodEvent>> = _events.asStateFlow()

    private val _priorities = MutableStateFlow<List<com.dinner.crimeapp.data.NeighbourhoodPriority>>(emptyList())
    val priorities: StateFlow<List<com.dinner.crimeapp.data.NeighbourhoodPriority>> = _priorities.asStateFlow()

    private val _savedPlaces = MutableStateFlow<List<SavedPlace>>(emptyList())
    val savedPlaces: StateFlow<List<SavedPlace>> = _savedPlaces.asStateFlow()

    private val _boundary = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val boundary: StateFlow<List<Pair<Double, Double>>> = _boundary.asStateFlow()

    private var areaJob: Job? = null
    private var currentAreaLat: Double? = null
    private var currentAreaLng: Double? = null

    /** Fetch and expose the neighbourhood boundary polygon for outline drawing. */
    fun loadBoundary(force: String, neighbourhood: String) {
        viewModelScope.launch {
            val pts = runCatching { repository.getBoundary(force, neighbourhood) }
                .getOrDefault(emptyList())
                .mapNotNull { p ->
                    val lat = p.latitude.toDoubleOrNull()
                    val lng = p.longitude.toDoubleOrNull()
                    if (lat != null && lng != null) lat to lng else null
                }
            _boundary.value = pts
        }
    }

    init {
        // Initialize offline accident data in the background
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            AccidentDataInitializer.initializeAccidentData(getApplication(), db.roadAccidentDao())
        }

        loadCategories()
        setupSearchDebounce()
        _savedPlaces.value = SavedPlacesStore.load(appContext)
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
        // Load area info immediately in parallel so Area tab is not delayed by crime queries
        loadAreaContext(lat, lng)

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

    /** Loads neighbourhood/force/reverse-geocoded place name for the current point. */
    fun loadAreaContext(lat: Double, lng: Double, forceRefresh: Boolean = false) {
        if (!forceRefresh && currentAreaLat == lat && currentAreaLng == lng && _areaInfo.value != null) {
            return
        }
        currentAreaLat = lat
        currentAreaLng = lng

        areaJob?.cancel()
        areaJob = viewModelScope.launch {
            _isAreaLoading.value = true
            _areaError.value = null
            _boundary.value = emptyList()

            val areaResult = runCatching { repository.getAreaInfo(lat, lng) }
            val info = areaResult.getOrNull()
            _areaInfo.value = info

            if (info != null) {
                launch { loadBoundary(info.forceId, info.neighbourhoodId) }
                launch {
                    _team.value = runCatching { repository.getTeam(info.forceId, info.neighbourhoodId) }
                        .getOrDefault(emptyList())
                }
                launch {
                    _events.value = runCatching { repository.getEvents(info.forceId, info.neighbourhoodId) }
                        .getOrDefault(emptyList())
                }
                launch {
                    _priorities.value = runCatching { repository.getPriorities(info.forceId, info.neighbourhoodId) }
                        .getOrDefault(emptyList())
                }
            } else {
                _team.value = emptyList()
                _events.value = emptyList()
                _priorities.value = emptyList()
                _areaError.value = areaResult.exceptionOrNull()?.message 
                    ?: "No local policing team found for this location."
            }

            launch {
                _areaLocationName.value = runCatching {
                    repository.reverseGeocode(lat, lng)?.let { resp ->
                        resp.address?.let { addr ->
                            listOfNotNull(addr["town"], addr["city"], addr["village"], addr["county"]).firstOrNull()
                        } ?: resp.name
                    }
                }.getOrNull()
            }

            _isAreaLoading.value = false
        }
    }

    fun reloadAreaContext() {
        val lat = currentAreaLat ?: return
        val lng = currentAreaLng ?: return
        repository.clearAreaCache()
        loadAreaContext(lat, lng, forceRefresh = true)
    }

    fun saveCurrentPlace(name: String, lat: Double, lng: Double) {
        _savedPlaces.value = SavedPlacesStore.add(appContext, name, lat, lng)
    }

    fun removeSavedPlace(id: String) {
        _savedPlaces.value = SavedPlacesStore.remove(appContext, id)
    }

    fun setViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun loadAccidentsForVisibleArea(mapView: MapView) {
        val bounds = mapView.boundingBox
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            val accidents = db.roadAccidentDao().getAccidentsInArea(
                minLat = bounds.latSouth,
                maxLat = bounds.latNorth,
                minLng = bounds.lonWest,
                maxLng = bounds.lonEast
            )
            _state.value = _state.value.copy(visibleAccidents = accidents)
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

    fun loadAreaProfile(lat: Double, lng: Double) {
        viewModelScope.launch {
            _areaProfileLoading.value = true
            _areaProfile.value = repository.getAreaProfile(lat, lng)
            _areaProfileLoading.value = false
        }
    }

    fun clearAreaProfile() {
        _areaProfile.value = null
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

    /** Convenience for insights screens — stats over currently visible crimes. */
    fun stats(): Stats = CrimeStats.compute(visibleCrimes())

    fun stopStats(): StopStats = CrimeStats.computeStopStats(visibleStopSearches())

    /** Months covered by the current dataset, newest first. */
    fun monthsCovered(): List<String> = state.value.crimesByMonth.keys.sortedDescending()

    fun areaLabel(): String = _areaLocationName.value
        ?: _areaInfo.value?.neighbourhoodName
        ?: _areaInfo.value?.forceName
        ?: "Selected area"
}
