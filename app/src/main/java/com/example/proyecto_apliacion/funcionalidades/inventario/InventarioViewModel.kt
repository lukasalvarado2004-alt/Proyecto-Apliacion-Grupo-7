package com.example.proyecto_apliacion.funcionalidades.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.Producto
import com.example.proyecto_apliacion.data.ProductoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventarioUiState(
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    val categoria: String = "Todos",
    val errorFormulario: String? = null,
    val mensaje: String? = null
) {
    val filtrados get() = productos.filter {
        (it.nombre.contains(busqueda, true) || it.id.contains(busqueda, true)) &&
            (categoria == "Todos" || it.categoria == categoria)
    }
}

class InventarioViewModel(private val repository: ProductoRepository) : ViewModel() {
    private val busqueda = MutableStateFlow("")
    private val categoria = MutableStateFlow("Todos")
    private val error = MutableStateFlow<String?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val uiState = combine(repository.observarProductos(), busqueda, categoria, error, mensaje) { productos, q, cat, err, msg ->
        InventarioUiState(productos, q, cat, err, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventarioUiState())

    init { viewModelScope.launch { repository.inicializarSiVacio() } }
    fun cambiarBusqueda(value: String) { busqueda.value = value }
    fun cambiarCategoria(value: String) { categoria.value = value }
    fun limpiarMensaje() { mensaje.value = null; error.value = null }

    fun guardar(id: String, nombre: String, categoria: String, stockTexto: String, precioTexto: String, imagenUri: String? = null) {
        val cleanId = id.trim().uppercase()
        val cleanNombre = nombre.trim()
        val stock = stockTexto.toIntOrNull()
        val precio = precioTexto.toIntOrNull()
        val err = when {
            cleanId.isBlank() -> "Ingresa un código de producto."
            !cleanId.matches(Regex("[A-Z0-9-]{3,12}")) -> "Usa 3–12 caracteres: letras, números o guion."
            cleanNombre.length < 3 -> "El nombre debe tener al menos 3 caracteres."
            categoria !in listOf("Ropa", "Mochilas", "Accesorios") -> "Selecciona una categoría válida."
            stock == null || stock < 0 || stock > 1_000_000 -> "El stock debe ser un número entre 0 y 1.000.000."
            precio == null || precio <= 0 -> "El precio debe ser mayor que cero."
            else -> null
        }
        if (err != null) { error.value = err; mensaje.value = null; return }
        viewModelScope.launch {
            repository.guardar(Producto(cleanId, cleanNombre, categoria, stock!!, precio!!, imagenUri))
            error.value = null
            mensaje.value = "Producto guardado en la base de datos local."
        }
    }
    fun eliminar(id: String) { viewModelScope.launch { repository.eliminar(id); mensaje.value = "Producto eliminado." } }
}
