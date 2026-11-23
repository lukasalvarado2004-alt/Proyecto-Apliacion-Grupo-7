package com.example.proyecto_apliacion.funcionalidades.comercial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.proyecto_apliacion.data.local.DetalleVenta
import com.example.proyecto_apliacion.data.local.KaijuDatabase
import com.example.proyecto_apliacion.data.local.Producto
import com.example.proyecto_apliacion.data.local.Venta
import com.example.proyecto_apliacion.data.repository.ComercialRepository
import com.example.proyecto_apliacion.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import com.example.proyecto_apliacion.data.local.Movimiento
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class VentasViewModel(
    private val comercialRepository: ComercialRepository,
    private val productoRepository: ProductoRepository,
    private val context: Context
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
        val cantidad = cantidadStr.toIntOrNull()
        if (producto == null || cantidad == null || cantidad <= 0) {
            _aviso.value = "Selecciona un producto y una cantidad válida"
            return
        }
        val productoId = producto.id
        viewModelScope.launch {
            try {
                val db = KaijuDatabase.obtener(context)
                db.withTransaction {
                    val dao = db.productoDao()
                    val actual = dao.buscarPorId(productoId)
                        ?: error("El producto ya no existe")
                    if (dao.descontarStock(productoId, cantidad) != 1) {
                        error("Stock insuficiente. Disponible: ${actual.stock}")
                    }
                    val total = actual.precio.toLong() * cantidad
                    val ventaId = comercialRepository.registrarVenta(
                        Venta(usuario = usuarioActual, total = total)
                    )
                    val detalle = DetalleVenta(
                        ventaId = ventaId,
                        productoId = actual.id,
                        nombreProducto = actual.nombre,
                        cantidad = cantidad,
                        precioUnitario = actual.precio,
                        subtotal = total
                    )
                    comercialRepository.registrarDetallesVenta(listOf(detalle))
                    db.movimientoDao().insertar(
                        Movimiento(
                            productoId = actual.id,
                            nombreProducto = actual.nombre,
                            tipo = "SALIDA",
                            cantidad = cantidad,
                            detalle = "Venta #${ventaId}"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aviso.value = "Error al registrar la venta: ${e.message}"
                return@launch
            }
            _aviso.value = "Venta registrada correctamente"
            onExito()
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
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VentasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VentasViewModel(comercialRepository, productoRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
