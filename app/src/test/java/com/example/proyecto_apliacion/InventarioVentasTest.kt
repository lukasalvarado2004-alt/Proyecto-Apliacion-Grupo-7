package com.example.proyecto_apliacion

import android.content.Context
import androidx.room.withTransaction
import com.example.proyecto_apliacion.data.local.*
import com.example.proyecto_apliacion.funcionalidades.comercial.VentasViewModel
import com.example.proyecto_apliacion.funcionalidades.inventario.InventarioViewModel
import com.example.proyecto_apliacion.funcionalidades.movimientos.MovimientosViewModel
import com.example.proyecto_apliacion.funcionalidades.panel_inicio.PanelInicioViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventarioVentasTest {
    @get:Rule internal val main = MainRule()
    private val datos = InventarioDePrueba()
    private val repo = datos.inventario
    private val p = Producto("ABC", "Bolso", "Accesorios", 5, 100)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockDb = mockk<KaijuDatabase>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(KaijuDatabase.Companion)
        every { KaijuDatabase.obtener(any()) } returns mockDb
        every { mockDb.productoDao() } returns datos.productoDao
        every { mockDb.movimientoDao() } returns datos.movimientoDao

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { mockDb.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            val bloque = secondArg<suspend () -> Any?>()
            bloque.invoke()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun altasEntradasSalidasYEdicionGeneranHistorial() = runTest {
        // Arrange
        val productoInicial = p

        // Act
        repo.guardar(productoInicial)
        repo.guardar(productoInicial.copy(stock = 9))
        repo.guardar(productoInicial.copy(stock = 7))
        repo.guardar(productoInicial.copy(stock = 7))
        repo.actualizarProducto(productoInicial.copy(nombre = "Bolso nuevo", stock = 7))
        repo.eliminar(productoInicial.id)
        repo.eliminar(productoInicial.id)

        // Assert
        assertEquals(
            listOf("ALTA", "ENTRADA", "SALIDA", "MODIFICACIÓN", "ELIMINACIÓN"),
            datos.historial.value.map { it.tipo }
        )
        assertEquals(listOf(5, 4, 2), datos.historial.value.take(3).map { it.cantidad })
        assertEquals("Datos del producto modificados", datos.historial.value[3].detalle)
        assertTrue(repo.observarProductos().first().isEmpty())
        assertEquals(5, repo.observarMovimientos().first().size)
    }

    @Test
    fun siFallaElMovimientoLanzaExcepcion() = runTest {
        // Arrange
        repo.guardar(p)
        datos.fallaMovimiento = true

        // Act
        val resultado = runCatching { repo.guardar(p.copy(stock = 99)) }

        // Assert
        assertTrue(resultado.isFailure)
        assertEquals(1, datos.historial.value.size)
    }

    @Test
    fun semillaEsIdempotente() = runTest {
        // Arrange & Act
        repo.inicializarSiVacio()
        val inicial = datos.productos.value
        repo.inicializarSiVacio()

        // Assert
        assertEquals(6, inicial.size)
        assertEquals(inicial, datos.productos.value)
    }

    @Test
    fun dosVentasConLaMismaSeleccionUsanPrecioYStockVigentes() = runTest {
        // Arrange
        repo.guardar(p)
        val vm = VentasViewModel(datos.comercial, repo, mockContext)
        val seleccionVieja = p.copy(stock = 20, precio = 1)
        var confirmadas = 0

        // Act
        repeat(2) {
            vm.registrarVenta("ana", seleccionVieja, "1") { confirmadas++ }
        }
        runCurrent()

        // Assert
        assertEquals(2, confirmadas)
        assertEquals(3, datos.productos.value.single().stock)
        assertEquals(listOf(100L, 100L), vm.ventas.first().map { it.total })
        assertEquals(200L, vm.ventasHoy.first())
        assertEquals(listOf(1L, 2L), datos.detalles.map { it.ventaId })
        assertEquals("SALIDA", datos.historial.value.last().tipo)
        assertTrue(vm.formatClp(1000L).contains("1.000"))
    }

    @Test
    fun ventaInvalidaInsuficienteOInexistenteNoConfirma() = runTest {
        // Arrange
        repo.guardar(p)
        val vm = VentasViewModel(datos.comercial, repo, mockContext)

        // Act & Assert: cantidades inválidas
        for (cantidad in listOf("x", "0", "-1", "6")) {
            vm.registrarVenta("v", p, cantidad) { fail("Venta inválida confirmada") }
            runCurrent()
            assertTrue(vm.aviso.value.isNotEmpty())
        }

        // Act & Assert: producto nulo o inexistente
        vm.registrarVenta("v", null, "1") { fail() }
        vm.registrarVenta("v", p.copy(id = "OTRO"), "1") { fail() }
        runCurrent()

        assertTrue(vm.aviso.value.contains("ya no existe"))
        assertTrue(datos.ventas.value.isEmpty())
        assertEquals(5, datos.productos.value.single().stock)

        vm.limpiarAviso()
        assertEquals("", vm.aviso.value)
    }

    @Test
    fun panelYMovimientosObservanCambios() = runTest {
        // Arrange
        repo.guardar(p)
        val panel = PanelInicioViewModel(repo)
        val historial = MovimientosViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { panel.resumenState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { historial.movimientos.collect() }

        // Act
        repo.guardar(p.copy(id = "OTRO", stock = 15))
        repo.guardar(p.copy(id = "EXTRA", stock = 16))
        runCurrent()

        // Assert
        assertEquals(36, panel.resumenState.value.stockTotal)
        assertEquals(2, panel.resumenState.value.productosBajoStock)
        assertEquals(3, historial.movimientos.value.size)
    }

    @Test
    fun formularioRechazaCamposInvalidosSinGuardar() = runTest {
        // Arrange
        repo.guardar(p)
        val vm = InventarioViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        // Act & Assert
        for (campos in listOf(
            listOf("", "Bolso", "Ropa", "4", "9"),
            listOf("?", "Bolso", "Ropa", "4", "9"),
            listOf("ABC", "x", "Ropa", "4", "9"),
            listOf("ABC", "Bolso", "Otra", "4", "9"),
            listOf("ABC", "Bolso", "Ropa", "-1", "9"),
            listOf("ABC", "Bolso", "Ropa", "4", "0")
        )) {
            vm.guardar(campos[0], campos[1], campos[2], campos[3], campos[4])
            runCurrent()
            assertNotNull(vm.uiState.value.errorFormulario)
        }

        assertEquals(listOf(p), datos.productos.value)
        vm.limpiarMensaje()
        runCurrent()
        assertNull(vm.uiState.value.errorFormulario)
    }

    @Test
    fun inventarioPermiteCrearFiltrarEditarYEliminar() = runTest {
        // Arrange
        repo.guardar(p)
        val vm = InventarioViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        // Act: crear y filtrar
        vm.guardar("  xyz ", " Polera ", "Ropa", "3", "250")
        advanceUntilIdle()
        vm.cambiarBusqueda("pol")
        vm.cambiarCategoria("Ropa")
        runCurrent()
        val creado = vm.uiState.value.filtrados.single()

        // Assert: creación
        assertEquals("XYZ", creado.id)

        // Act: editar
        vm.seleccionarProductoParaEditar(creado)
        assertEquals(creado, vm.productoAEditar)
        vm.actualizar("XYZ", "Polera azul", "Ropa", "4", "300", null)
        advanceUntilIdle()

        // Assert: edición
        assertEquals(4, vm.uiState.value.filtrados.single().stock)

        // Act: eliminar
        vm.eliminar("XYZ")
        advanceUntilIdle()

        // Assert: eliminación
        assertTrue(vm.uiState.value.filtrados.isEmpty())
        assertFalse(vm.uiState.value.cargando)
    }
}