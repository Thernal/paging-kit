package io.thernal.pagingkit.sample.shared.windows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.thernal.pagingkit.paging.api.domain.loader.PageLoader
import io.thernal.pagingkit.paging.api.domain.model.PagingState
import io.thernal.pagingkit.paging.api.domain.paginator.Paginator
import io.thernal.pagingkit.paging.api.domain.paginator.PaginatorFactory
import io.thernal.pagingkit.sample.shared.fake.FakeServer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val firstNames = listOf(
    "Adil", "Aysel", "Babək", "Cavid", "Dilarə", "Elvin", "Fidan", "Günay", "Həsən", "İlkin",
    "Kamran", "Leyla", "Murad", "Nigar", "Orxan", "Pərvin", "Rəşad", "Səbinə", "Tural", "Ülviyyə",
    "Vüsal", "Yaqub", "Zaur",
)

private val lastNames = listOf("Əliyev", "Həsənov", "Məmmədov", "Quliyev", "Rzayev", "Səfərov")

/**
 * Contacts arrive sorted by name, fifteen at a time — so one letter's contacts are regularly split
 * across two pages, and the header for it must not appear twice.
 */
class ContactsViewModel(
    paginatorFactory: PaginatorFactory,
) : ViewModel() {
    private val server = FakeServer {
        firstNames
            .flatMap { first -> lastNames.map { last -> "$first $last" } }
            .sorted()
            .mapIndexed { index, name -> Contact(id = index, name = name, phone = "+994 50 ${100 + index} 00 00") }
    }

    private val paginator: Paginator<Contact> = paginatorFactory.create(
        loader = PageLoader { page, size -> server.load(page = page, size = size) },
        identity = { contact -> contact.id },
    )

    val state: StateFlow<PagingState<Contact>> = paginator.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PagingState.Idle,
    )

    init {
        onFetch()
    }

    fun onFetch() {
        viewModelScope.launch { paginator.fetch(size = CONTACTS_PAGE_SIZE) }
    }
}

private const val CONTACTS_PAGE_SIZE = 15
