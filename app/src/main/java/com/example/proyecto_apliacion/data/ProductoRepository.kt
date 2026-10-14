package com.example.proyecto_apliacion.data

import kotlinx.coroutines.flow.Flow

class ProductoRepository(private val dao: ProductoDao, private val movimientosDao: MovimientoDao) {
    fun observarProductos(): Flow<List<Producto>> = dao.observarTodos()
    fun observarMovimientos(): Flow<List<Movimiento>> = movimientosDao.observarTodos()

    suspend fun guardar(producto: Producto) {
        val anterior = dao.buscarPorId(producto.id)
        dao.guardar(producto)
        if (anterior == null) {
            movimientosDao.insertar(Movimiento(productoId = producto.id, nombreProducto = producto.nombre,
                tipo = "ALTA", cantidad = producto.stock, detalle = "Producto creado en inventario"))
        } else if (anterior.stock != producto.stock) {
            val diferencia = producto.stock - anterior.stock
            movimientosDao.insertar(Movimiento(productoId = producto.id, nombreProducto = producto.nombre,
                tipo = if (diferencia > 0) "ENTRADA" else "SALIDA", cantidad = kotlin.math.abs(diferencia),
                detalle = "Actualización de stock"))
        }
    }

    suspend fun eliminar(id: String) {
        val producto = dao.buscarPorId(id)
        if (producto != null) {
            movimientosDao.insertar(Movimiento(productoId = producto.id, nombreProducto = producto.nombre,
                tipo = "ELIMINACIÓN", cantidad = producto.stock, detalle = "Producto eliminado del inventario"))
            dao.eliminar(id)
        }
    }

    suspend fun inicializarSiVacio() {
        if (dao.contar() == 0) listOf(
            Producto("K001", "Polera Kaiju Negra", "Ropa", 520, 14990),
            Producto("K002", "Polerón Crop Verde", "Ropa", 400, 29990),
            Producto("K003", "Mochila Urbana Escolar", "Mochilas", 150, 34990),
            Producto("K004", "Gorra Trucker Logo", "Accesorios", 85, 9990),
            Producto("K005", "Banano Impermeable", "Accesorios", 12, 12990),
            Producto("K006", "Mochila de Viaje Táctica", "Mochilas", 9, 49990)
        ).forEach { dao.guardar(it) }
    }
}
