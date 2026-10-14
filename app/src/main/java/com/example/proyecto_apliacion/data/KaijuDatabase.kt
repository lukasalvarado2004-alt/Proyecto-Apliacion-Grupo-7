package com.example.proyecto_apliacion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Producto::class, Movimiento::class], version = 2, exportSchema = false)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN imagenUri TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS movimientos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, productoId TEXT NOT NULL, nombreProducto TEXT NOT NULL, tipo TEXT NOT NULL, cantidad INTEGER NOT NULL, detalle TEXT NOT NULL, fechaMillis INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: KaijuDatabase? = null
        fun obtener(context: Context): KaijuDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, KaijuDatabase::class.java, "kaiju_inventory.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
