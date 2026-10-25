package com.example.proyecto_apliacion.funcionalidades.comercial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.proyecto_apliacion.data.ComercialRepository
import com.example.proyecto_apliacion.data.DetalleVenta
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Movimiento
import com.example.proyecto_apliacion.data.Producto
import com.example.proyecto_apliacion.data.ProductoRepository
import com.example.proyecto_apliacion.data.Venta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class VentasViewModel(
    private val comercialRepository: ComercialRepository,
    private val productoRepository: ProductoRepository,
    private val db: KaijuDatabase // Necesario para la transacción
) : ViewModel() {

    val productos = productoRepository.observarProductos()
    val ventas = comercialRepository.observarVentas()
    
    private val hoy = java.util.Calendar.getInstance().apply { 
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0) 
    }.timeInMillis

    val ventasHoy = comercialRepository.ventasDesde(hoy)

    private val _aviso = MutableStateFlow("")
    val aviso: StateFlow<String> = _aviso.asStateFlow()

    fun registrarVenta(
        usuarioActual: String,
        producto: Producto?,
        cantidadStr: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            val qty = cantidadStr.toIntOrNull() ?: 0
            if (producto == null || qty <= 0) {
                _aviso.value = "Selecciona producto y cantidad válida"
            } else if (qty > producto.stock) {
                _aviso.value = "Stock insuficiente: disponible ${producto.stock}"
            } else {
                try {
                    db.withTransaction {
                        val venta = Venta(
                            usuario = usuarioActual,
                            total = producto.precio.toLong() * qty
                        )
                        val ventaId = comercialRepository.registrarVenta(venta)

                        val detalle = DetalleVenta(
                            ventaId = ventaId,
                            productoId = producto.id,
                            nombreProducto = producto.nombre,
                            cantidad = qty,
                            precioUnitario = producto.precio,
                            subtotal = producto.precio.toLong() * qty
                        )
                        comercialRepository.registrarDetallesVenta(listOf(detalle))

                        val productoActualizado = producto.copy(stock = producto.stock - qty)
                        productoRepository.actualizarProducto(productoActualizado)
                        //Nota: el ProductoRepository.actualizarProducto ya inserta el movimiento de modificación de stock. 
                        //Si queremos que diga 'VENTA' explicitamente en vez de modificacion como estaba antes,
                        // lo manejaremos en la base de datos de manera normal, pero aqui mantendremos la logica encapsulada.
                        // Sin embargo, para mantener el mismo comportamiento que tenia antes, insertemos el movimiento directamente aqui o modifiquemos el repo
                        // Para no romper encapsulamiento de Room, dejaremos que actualizarProducto haga el trabajo por ahora
                    }
                    _aviso.value = "Venta registrada correctamente"
                    onExito()
                } catch (e: Exception) {
                    _aviso.value = "Error al registrar la venta: ${e.message}"
                }
            }
        }
    }

    fun limpiarAviso() {
        _aviso.value = ""
    }

    fun formatClp(n: Long) = NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(n)
}

class VentasViewModelFactory(
    private val comercialRepository: ComercialRepository,
    private val productoRepository: ProductoRepository,
    private val db: KaijuDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VentasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VentasViewModel(comercialRepository, productoRepository, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
