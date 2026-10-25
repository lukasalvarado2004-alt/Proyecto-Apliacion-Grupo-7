package com.example.proyecto_apliacion.funcionalidades.panel_inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.ProductoRepository
import kotlinx.coroutines.launch

class PanelInicioViewModel(private val repository: ProductoRepository) : ViewModel() {

    val productos = repository.observarProductos()

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