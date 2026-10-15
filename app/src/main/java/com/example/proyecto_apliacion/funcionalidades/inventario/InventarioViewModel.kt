package com.example.proyecto_apliacion.funcionalidades.inventario

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.Producto
import com.example.proyecto_apliacion.data.ProductoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventarioUiState(
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    val categoria: String = "Todos",
    val errorFormulario: String? = null,
    val mensaje: String? = null,
    val cargando: Boolean = false
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
    private val cargando = MutableStateFlow(false)

    // Estado para controlar qué producto se está editando actualmente (null = ninguno)
    var productoAEditar by mutableStateOf<Producto?>(null)
        private set

    // Selecciona el producto a editar (o null para cerrar el diálogo)
    fun seleccionarProductoParaEditar(producto: Producto?) {
        productoAEditar = producto
    }

    // Combinamos todos los flujos de estado incluyendo 'cargando'
    val uiState = combine(
        repository.observarProductos(),
        busqueda,
        categoria,
        error,
        mensaje,
        cargando
    ) { array ->
        InventarioUiState(
            productos = array[0] as List<Producto>,
            busqueda = array[1] as String,
            categoria = array[2] as String,
            errorFormulario = array[3] as String?,
            mensaje = array[4] as String?,
            cargando = array[5] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventarioUiState())

    init { viewModelScope.launch { repository.inicializarSiVacio() } }
    fun cambiarBusqueda(value: String) { busqueda.value = value }
    fun cambiarCategoria(value: String) { categoria.value = value }
    fun limpiarMensaje() { mensaje.value = null; error.value = null }

    // CREACIÓN
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
            cargando.value = true
            delay(600) // Pequeña pausa para visualizar la animación
            repository.guardar(Producto(cleanId, cleanNombre, categoria, stock!!, precio!!, imagenUri))
            error.value = null
            cargando.value = false
            mensaje.value = "Creación exitosa"
        }
    }

    // MODIFICACIÓN
    fun actualizar(id: String, nombre: String, categoria: String, stock: String, precio: String, imagenUri: String?) {
        val stockInt = stock.toIntOrNull() ?: 0
        val precioConvertido = precio.toIntOrNull() ?: 0

        viewModelScope.launch {
            cargando.value = true
            delay(600) // Pequeña pausa para visualizar la animación
            val productoEditado = Producto(
                id = id,
                nombre = nombre,
                categoria = categoria,
                stock = stockInt,
                precio = precioConvertido,
                imagenUri = imagenUri
            )
            repository.actualizarProducto(productoEditado)
            cargando.value = false
            mensaje.value = "Modificación exitosa"
        }
    }

    // ELIMINACIÓN
    fun eliminar(id: String) {
        viewModelScope.launch {
            cargando.value = true
            delay(600) // Pequeña pausa para visualizar la animación
            repository.eliminar(id)
            cargando.value = false
            mensaje.value = "Eliminación exitosa"
        }
    }
}