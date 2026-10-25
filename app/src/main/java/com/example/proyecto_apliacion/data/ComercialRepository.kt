package com.example.proyecto_apliacion.data

import kotlinx.coroutines.flow.Flow

class ComercialRepository(
    private val ventaDao: VentaDao,
    private val usuarioDao: UsuarioDao,
    private val categoriaDao: CategoriaDao
) {
    // --- Ventas ---
    fun observarVentas(): Flow<List<Venta>> = ventaDao.observar()
    fun ventasDesde(fechaMillis: Long): Flow<Long> = ventaDao.ventasDesde(fechaMillis)
    
    suspend fun registrarVenta(venta: Venta): Long {
        return ventaDao.insertar(venta)
    }

    suspend fun registrarDetallesVenta(detalles: List<DetalleVenta>) {
        ventaDao.insertarDetalles(detalles)
    }

    // --- Usuarios ---
    fun observarUsuarios(): Flow<List<Usuario>> = usuarioDao.observar()
    
    suspend fun contarUsuarios(): Int = usuarioDao.contar()
    
    suspend fun guardarUsuario(usuario: Usuario) {
        usuarioDao.guardar(usuario)
    }
    
    suspend fun cambiarEstadoUsuario(username: String, activo: Boolean) {
        usuarioDao.cambiarEstado(username, activo)
    }
    
    suspend fun eliminarUsuario(username: String) {
        usuarioDao.eliminar(username)
    }

    // --- Categorias ---
    fun observarCategorias(): Flow<List<Categoria>> = categoriaDao.observar()
    
    suspend fun contarCategorias(): Int = categoriaDao.contar()
    
    suspend fun guardarCategoria(categoria: Categoria) {
        categoriaDao.guardar(categoria)
    }
    
    suspend fun cambiarEstadoCategoria(nombre: String, activa: Boolean) {
        categoriaDao.cambiarEstado(nombre, activa)
    }
    
    suspend fun eliminarCategoria(nombre: String) {
        categoriaDao.eliminar(nombre)
    }
}