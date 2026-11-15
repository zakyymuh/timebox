package dev.zakyymuh.timebox.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.Sale
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.Sales
import dev.zakyymuh.timebox.core.collection.SalesTotals
import dev.zakyymuh.timebox.data.SaleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What is up for sale, every sale newest first, and what they add up to. */
data class SalesState(val totals: SalesTotals, val forSale: List<DexEntry>, val sold: List<Sale>)

/** The sales page. [state] is null until loaded, so the empty hints never flash. */
class SalesViewModel(private val repo: SaleRepository) : ViewModel() {
    val state: StateFlow<SalesState?> = combine(repo.forSaleFlow(), repo.salesFlow()) { forSale, sold ->
        SalesState(Sales.totals(sold, repo.todayDate()), forSale, sold)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun edit(id: Long, form: SaleForm) {
        viewModelScope.launch { repo.edit(id, form) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
