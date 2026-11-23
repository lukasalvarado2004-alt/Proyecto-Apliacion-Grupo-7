package com.example.proyecto_apliacion

import com.example.proyecto_apliacion.data.local.*
import com.example.proyecto_apliacion.data.repository.*
import kotlinx.coroutines.flow.*

internal class InventarioDePrueba {
    val productos = MutableStateFlow(emptyList<Producto>())
    val historial = MutableStateFlow(emptyList<Movimiento>())
    val ventas = MutableStateFlow(emptyList<Venta>())
    val detalles = mutableListOf<DetalleVenta>()
    val categorias = MutableStateFlow(emptyList<Categoria>())
    var fallaMovimiento = false

     val productoDao = object : ProductoDao {
        override fun observarTodos() = productos
        override suspend fun buscarPorId(id: String) = productos.value.find { it.id == id }
        override suspend fun contar() = productos.value.size
        override suspend fun guardar(producto: Producto) {
            productos.value = productos.value.filterNot { it.id == producto.id } + producto
        }
        override suspend fun updateProducto(producto: Producto) {
            if (buscarPorId(producto.id) != null) guardar(producto)
        }
        override suspend fun eliminar(id: String) {
            productos.value = productos.value.filterNot { it.id == id }
        }
        override suspend fun descontarStock(id: String, cantidad: Int): Int {
            val p = buscarPorId(id) ?: return 0
            if (cantidad <= 0 || p.stock < cantidad) return 0
            guardar(p.copy(stock = p.stock - cantidad))
            return 1
        }
    }

     val movimientoDao = object : MovimientoDao {
        override fun observarTodos() = historial
        override suspend fun insertar(movimiento: Movimiento) {
            if (fallaMovimiento) throw IllegalStateException("Fallo de historial")
            historial.value += movimiento
        }
    }

    val inventario = ProductoRepository(
        productodao = productoDao,
        movimientosDao = movimientoDao
    )

    val usuarios = UsuariosEnMemoria()

    val comercial = ComercialRepository(
        object : VentaDao {
            override fun observar() = ventas
            override suspend fun detalles(ventaId: Long) = detalles.filter { it.ventaId == ventaId }
            override suspend fun insertar(venta: Venta): Long {
                val id = ventas.value.size + 1L
                ventas.value += venta.copy(id = id)
                return id
            }
            override suspend fun insertarDetalles(detalles: List<DetalleVenta>) {
                this@InventarioDePrueba.detalles.addAll(detalles)
            }
            override fun ventasDesde(fechaMillis: Long) = ventas.map { lista ->
                lista.filter { it.fechaMillis >= fechaMillis }.sumOf { it.total }
            }
            override fun contarVentas() = ventas.map { it.size }
        },
        usuarios,
        object : CategoriaDao {
            override fun observar() = categorias
            override suspend fun contar() = categorias.value.size
            override suspend fun guardar(categoria: Categoria) {
                categorias.value = categorias.value.filterNot { it.nombre == categoria.nombre } + categoria
            }
            override suspend fun cambiarEstado(nombre: String, activa: Boolean) {
                categorias.value = categorias.value.map { if (it.nombre == nombre) it.copy(activa = activa) else it }
            }
            override suspend fun eliminar(nombre: String) {
                categorias.value = categorias.value.filterNot { it.nombre == nombre }
            }
        }
    )
}

internal class UsuariosEnMemoria : UsuarioDao {
    val datos = MutableStateFlow(emptyList<Usuario>())
    override fun observar() = datos
    override suspend fun contar() = datos.value.size
    override suspend fun guardar(usuario: Usuario) {
        datos.value = datos.value.filterNot { it.username == usuario.username } + usuario
    }
    override suspend fun eliminar(username: String) {
        datos.value = datos.value.filterNot { it.username == username }
    }
    override suspend fun buscarPorCredenciales(usuario: String, clave: String) =
        datos.value.find { (it.username == usuario || it.correo == usuario) && it.clave == clave && it.activo }
    override suspend fun buscarPorCorreo(correo: String) = datos.value.find { it.correo.equals(correo, true) }
    override suspend fun buscarPorNombre(username: String) = datos.value.find { it.username == username }
    override suspend fun registrar(usuario: Usuario) = guardar(usuario)
    override suspend fun actualizar(usuario: Usuario) = guardar(usuario)
    override suspend fun cambiarEstado(username: String, activo: Boolean) {
        buscarPorNombre(username)?.let { guardar(it.copy(activo = activo)) }
    }
}