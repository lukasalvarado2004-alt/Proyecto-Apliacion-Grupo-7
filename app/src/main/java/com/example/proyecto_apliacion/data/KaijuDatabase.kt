package com.example.proyecto_apliacion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Producto::class, Movimiento::class, Usuario::class, Categoria::class, Venta::class, DetalleVenta::class], version = 4, exportSchema = false)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun ventaDao(): VentaDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN imagenUri TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS movimientos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, productoId TEXT NOT NULL, nombreProducto TEXT NOT NULL, tipo TEXT NOT NULL, cantidad INTEGER NOT NULL, detalle TEXT NOT NULL, fechaMillis INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (username TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, rol TEXT NOT NULL, activo INTEGER NOT NULL, correo TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS categorias (nombre TEXT NOT NULL PRIMARY KEY, descripcion TEXT NOT NULL, activa INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS ventas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fechaMillis INTEGER NOT NULL, usuario TEXT NOT NULL, total INTEGER NOT NULL, estado TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS detalle_ventas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ventaId INTEGER NOT NULL, productoId TEXT NOT NULL, nombreProducto TEXT NOT NULL, cantidad INTEGER NOT NULL, precioUnitario INTEGER NOT NULL, subtotal INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO usuarios(username,nombre,rol,activo,correo) VALUES ('admin','Administrador','ADMIN',1,'admin@kaiju.cl'),('ventas','Personal de ventas','VENTAS',1,'ventas@kaiju.cl'),('inventario','Encargado de inventario','INVENTARIO',1,'inventario@kaiju.cl')")
                db.execSQL("INSERT OR IGNORE INTO categorias(nombre,descripcion,activa) VALUES ('Ropa','Prendas y vestuario',1),('Mochilas','Mochilas y bolsos',1),('Accesorios','Accesorios Kaiju',1)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN clave TEXT NOT NULL DEFAULT ''")
            }
        }
        @Volatile private var INSTANCE: KaijuDatabase? = null
        fun obtener(context: Context): KaijuDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, KaijuDatabase::class.java, "kaiju_inventory.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
        }
    }
}
