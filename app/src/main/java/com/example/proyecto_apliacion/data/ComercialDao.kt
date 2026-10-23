package com.example.proyecto_apliacion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao interface UsuarioDao {
 @Query("SELECT * FROM usuarios ORDER BY username") fun observar(): Flow<List<Usuario>>
 @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun guardar(usuario: Usuario)
 @Query("UPDATE usuarios SET activo = :activo WHERE username = :username") suspend fun cambiarEstado(username: String, activo: Boolean)
 @Query("DELETE FROM usuarios WHERE username = :username") suspend fun eliminar(username: String)
 @Query("SELECT COUNT(*) FROM usuarios") suspend fun contar(): Int
 @Query("SELECT * FROM usuarios WHERE (username = :usuario OR correo = :usuario) AND clave = :clave AND activo = 1 LIMIT 1") suspend fun buscarPorCredenciales(usuario: String, clave: String): Usuario?
 @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1") suspend fun buscarPorCorreo(correo: String): Usuario?
 @Insert(onConflict = androidx.room.OnConflictStrategy.ABORT) suspend fun registrar(usuario: Usuario)
}
@Dao interface CategoriaDao {
 @Query("SELECT * FROM categorias ORDER BY nombre") fun observar(): Flow<List<Categoria>>
 @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun guardar(categoria: Categoria)
 @Query("UPDATE categorias SET activa = :activa WHERE nombre = :nombre") suspend fun cambiarEstado(nombre: String, activa: Boolean)
 @Query("DELETE FROM categorias WHERE nombre = :nombre") suspend fun eliminar(nombre: String)
 @Query("SELECT COUNT(*) FROM categorias") suspend fun contar(): Int
}
@Dao interface VentaDao {
 @Query("SELECT * FROM ventas ORDER BY fechaMillis DESC") fun observar(): Flow<List<Venta>>
 @Query("SELECT * FROM detalle_ventas WHERE ventaId = :ventaId") suspend fun detalles(ventaId: Long): List<DetalleVenta>
 @Insert suspend fun insertar(venta: Venta): Long
 @Insert suspend fun insertarDetalles(detalles: List<DetalleVenta>)
 @Query("SELECT COALESCE(SUM(total),0) FROM ventas WHERE fechaMillis >= :desde") fun ventasDesde(desde: Long): Flow<Long>
 @Query("SELECT COUNT(*) FROM ventas") fun contarVentas(): Flow<Int>
}
