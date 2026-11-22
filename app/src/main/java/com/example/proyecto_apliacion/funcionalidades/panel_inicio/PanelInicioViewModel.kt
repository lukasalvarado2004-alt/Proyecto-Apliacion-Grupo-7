package com.example.proyecto_apliacion.funcionalidades.panel_inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.repository.ProductoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResumenPanelState(
    val stockTotal: Int = 0,
    val productosBajoStock: Int = 0
)

class PanelInicioViewModel(private val repository: ProductoRepository) : ViewModel() {

    val resumenState = repository.observarProductos()
        .map { lista ->
            ResumenPanelState(
                stockTotal = lista.sumOf { it.stock },
                productosBajoStock = lista.count { it.stock <= 15 }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ResumenPanelState()
        )

    init {
        viewModelScope.launch {
            repository.inicializarSiVacio()
        }
    }
}

class PanelInicioViewModelFactory(private val repository: ProductoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PanelInicioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PanelInicioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}