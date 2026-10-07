package io.thernal.pagingkit.sample.shared.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.shared.fake.FakeServer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val cities = listOf(
    "Ağdam", "Ağstafa", "Astara", "Bakı", "Balakən", "Bərdə", "Beyləqan", "Cəlilabad", "Daşkəsən", "Füzuli",
    "Gəncə", "Goranboy", "Göyçay", "Hacıqabul", "İmişli", "İsmayıllı", "Kəlbəcər", "Laçın", "Lənkəran",
    "Masallı", "Mingəçevir", "Naftalan", "Naxçıvan", "Oğuz", "Qax", "Qazax", "Qəbələ", "Quba", "Qusar",
    "Saatlı", "Sabirabad", "Şabran", "Şamaxı", "Şəki", "Şəmkir", "Şirvan", "Şuşa", "Sumqayıt", "Tərtər",
    "Tovuz", "Ucar", "Xaçmaz", "Xankəndi", "Xızı", "Yardımlı", "Yevlax", "Zaqatala", "Zəngilan", "Zərdab",
)

private const val RESULTS_PER_CITY = 8

private val searchDebounce = 350.milliseconds

/**
 * Search as you type. The loader reads [activeQuery], so a new query is a `reset` and a fetch — one
 * paginator for the life of the screen, not one per query. A reset while the previous query's page
 * is still in flight discards that page when it returns, so a slow answer to "Ba" can never land
 * under "Bakı".
 */
class SearchViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    // The query the current pages belong to — not the text field, which runs ahead of the debounce.
    // Read live, a page the list asked for between a keystroke and the reset would load for the new
    // query and be appended under the old one's results.
    private var activeQuery = ""

    private val server = FakeServer(latency = 900.milliseconds) {
        val current = activeQuery.trim()
        cities
            .filter { city -> city.contains(other = current, ignoreCase = true) }
            .flatMap { city -> (1..RESULTS_PER_CITY).map { n -> Place(id = "$city-$n", name = "$city, district $n") } }
    }

    private val paginator: Paginator<Place> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { place -> place.id },
    )

    val state: StateFlow<PagingState<Place>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    init {
        onFetch()
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            mutableQuery.drop(1).debounce(searchDebounce).collect { query ->
                activeQuery = query
                paginator.reset()
                onFetch()
            }
        }
    }

    fun onQueryChange(query: String) {
        mutableQuery.value = query
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch() }
    }
}
