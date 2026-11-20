package com.example.proyecto_apliacion.data.repository

import com.example.proyecto_apliacion.data.local.Movimiento
import com.example.proyecto_apliacion.data.local.MovimientoDao
import com.example.proyecto_apliacion.data.local.Producto
import com.example.proyecto_apliacion.data.local.ProductoDao
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

class ProductoRepository(private val productodao: ProductoDao, private val movimientosDao: MovimientoDao) {
    fun observarProductos(): Flow<List<Producto>> = productodao.observarTodos()
    fun observarMovimientos(): Flow<List<Movimiento>> = movimientosDao.observarTodos()

    suspend fun guardar(producto: Producto) {
        val anterior = productodao.buscarPorId(producto.id)
        productodao.guardar(producto)
        if (anterior == null) {
            movimientosDao.insertar(
                Movimiento(
                    productoId = producto.id,
                    nombreProducto = producto.nombre,
                    tipo = "ALTA",
                    cantidad = producto.stock,
                    detalle = "Producto creado en inventario"
                )
            )
        } else if (anterior.stock != producto.stock) {
            val diferencia = producto.stock - anterior.stock
            movimientosDao.insertar(
                Movimiento(
                    productoId = producto.id, nombreProducto = producto.nombre,
                    tipo = if (diferencia > 0) "ENTRADA" else "SALIDA", cantidad = abs(diferencia),
                    detalle = "Actualización de stock"
                )
            )
        }
    }
    suspend fun actualizarProducto(producto: Producto) {
        val anterior = productodao.buscarPorId(producto.id)
        productodao.updateProducto(producto)

        if (anterior != null) {
            val difStock = producto.stock - anterior.stock
            val detalleTexto = if (difStock != 0) {
                "Cambio de stock (${anterior.stock} → ${producto.stock})"
            } else {
                "Datos del producto modificados"
            }

            movimientosDao.insertar(
                Movimiento(
                    productoId = producto.id,
                    nombreProducto = producto.nombre,
                    tipo = "MODIFICACIÓN",
                    cantidad = if (difStock != 0) abs(difStock) else producto.stock,
                    detalle = detalleTexto
                )
            )
        }
    }
    suspend fun eliminar(id: String) {
        val producto = productodao.buscarPorId(id)
        if (producto != null) {
            movimientosDao.insertar(
                Movimiento(
                    productoId = producto.id,
                    nombreProducto = producto.nombre,
                    tipo = "ELIMINACIÓN",
                    cantidad = producto.stock,
                    detalle = "Producto eliminado del inventario"
                )
            )
            productodao.eliminar(id)
        }
    }



    suspend fun inicializarSiVacio() {
        if (productodao.contar() == 0) listOf(
            Producto("K001", "Polera Kaiju Negra", "Ropa", 520, 14990),
            Producto("K002", "Polerón Crop Verde", "Ropa", 400, 29990),
            Producto("K003", "Mochila Urbana Escolar", "Mochilas", 150, 34990),
            Producto("K004", "Gorra Trucker Logo", "Accesorios", 85, 9990),
            Producto("K005", "Banano Impermeable", "Accesorios", 12, 12990),
            Producto("K006", "Mochila de Viaje Táctica", "Mochilas", 9, 49990)
        ).forEach { productodao.guardar(it) }
    }
}