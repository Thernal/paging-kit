package io.thernal.pagingkit.sample.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which example is open, and the ViewModel store its screens live in.
 *
 * Every example owns its paginators through a ViewModel, the way an application screen does. Those
 * ViewModels must survive a rotation — so the store cannot be remembered by the composition — and
 * must be cleared when the example closes, so reopening it starts from page `0` rather than where
 * the reader left it. Holding the store here gives both.
 */
class RootViewModel : ViewModel() {
    private val mutableOpenExampleId = MutableStateFlow<String?>(null)
    val openExampleId: StateFlow<String?> = mutableOpenExampleId.asStateFlow()

    private var exampleStore = ViewModelStore()

    val exampleStoreOwner: ViewModelStoreOwner
        get() {
            val store = exampleStore
            return object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = store
            }
        }

    fun onOpen(exampleId: String) {
        mutableOpenExampleId.value = exampleId
    }

    fun onClose() {
        exampleStore.clear()
        exampleStore = ViewModelStore()
        mutableOpenExampleId.value = null
    }

    override fun onCleared() {
        exampleStore.clear()
    }
}
