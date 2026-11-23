package com.example.proyecto_apliacion

import android.content.Context
import com.example.proyecto_apliacion.data.DefaultAppContainer
import com.example.proyecto_apliacion.data.remote.ApiService
import com.example.proyecto_apliacion.data.remote.SessionManager
import com.example.proyecto_apliacion.data.repository.DefaultUsuarioRepository
import com.example.proyecto_apliacion.funcionalidades.autenticacion.LoginViewModel
import com.example.proyecto_apliacion.funcionalidades.comercial.AdministracionViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SesionAdministracionTest {
    @get:Rule internal val main = MainRule()

    private val datos = InventarioDePrueba()
    private val api = ApiUsuariosPrueba()
    private val repo = DefaultUsuarioRepository(datos.usuarios, api)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockSessionManager = mockk<SessionManager>(relaxed = true)

    @Before
    fun setUp() {
        mockkConstructor(DefaultAppContainer::class)
        every { anyConstructed<DefaultAppContainer>().apiService } returns mockk(relaxed = true)
        every { anyConstructed<DefaultAppContainer>().sessionManager } returns mockSessionManager
        every { anyConstructed<DefaultAppContainer>().usuarioRepository } returns repo
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loginOperadorDemoConClaveCorrectaIniciaSesion() {
        // Arrange
        val login = LoginViewModel()
        login.onUsuarioCambiado("  ADMIN ")
        login.onContrasenaCambiada("1234")

        // Act
        login.iniciarSesion(mockContext)

        // Assert
        assertTrue(login.loginExitoso)
        assertFalse(login.esIngresoCliente)
        assertNull(login.mensajeError)
        verify { mockSessionManager.guardarRol("ADMINISTRADOR") }

        login.consumirLoginExitoso()
        assertFalse(login.loginExitoso)
        assertEquals("", login.usuario)
        assertEquals("", login.contrasena)
    }

    @Test
    fun operadorDemoRechazaClaveIncorrectaYCamposVacios() {
        // Arrange
        val login = LoginViewModel()

        // Act 1: campos vacíos
        login.iniciarSesion(mockContext)

        // Assert 1
        assertTrue(login.mensajeError!!.contains("campos"))
        assertFalse(login.loginExitoso)

        // Act 2: clave incorrecta
        login.onUsuarioCambiado("admin")
        login.onContrasenaCambiada("incorrecta")
        login.iniciarSesion(mockContext)

        // Assert 2
        assertFalse(login.loginExitoso)
        assertEquals("Usuario o contraseña incorrectos.", login.mensajeError)
    }

    @Test
    fun loginRemotoGuardaTokenYRolDeCliente() = runTest {
        // Arrange
        val mockApiService = mockk<ApiService>()
        coEvery { mockApiService.login(any()) } returns tokenPrueba
        every { anyConstructed<DefaultAppContainer>().apiService } returns mockApiService

        val login = LoginViewModel()
        login.onUsuarioCambiado("ANA@test.cl")
        login.onContrasenaCambiada("secreto")

        // Act
        login.iniciarSesion(mockContext)
        runCurrent()

        // Assert
        assertTrue(login.loginExitoso)
        assertTrue(login.esIngresoCliente)
        verify { mockSessionManager.guardarToken("token-prueba") }
        verify { mockSessionManager.guardarRol("CLIENTE") }
    }

    @Test
    fun registroValidaCamposComunicaConflictoYConfirmaExito() = runTest {
        // Arrange
        val login = LoginViewModel()
        var mensaje = ""
        var exitos = 0

        // Act 1: validación campos vacíos
        login.registrarUsuario(mockContext, "", cliente.correo, cliente.clave, { exitos++ }, { mensaje = it })

        // Assert 1
        assertTrue(mensaje.contains("campos"))

        // Act 2: conflicto 409
        api.crear = {
            Response.error(409, "Duplicado".toResponseBody("text/plain".toMediaTypeOrNull()))
        }
        login.registrarUsuario(mockContext, "Ana", cliente.correo, cliente.clave, { exitos++ }, { mensaje = it })
        runCurrent()

        // Assert 2
        assertTrue(mensaje.contains("ya está registrado"))
        assertEquals(0, exitos)

        // Act 3: éxito
        api.crear = { Response.success(remoto) }
        login.registrarUsuario(mockContext, " Ana ", cliente.correo, cliente.clave, { exitos++ }, { mensaje = it })
        runCurrent()

        // Assert 3
        assertEquals(1, exitos)
    }

    @Test
    fun administracionSincronizaCreaEditaYElimina() = runTest {
        // Arrange
        val vm = AdministracionViewModel(repo, datos.comercial)
        runCurrent()
        assertEquals(3, vm.categorias.first().size)
        assertEquals(1, vm.usuarios.first().size)

        // Act & Assert: validación creación
        var exitos = 0
        vm.guardarUsuario("", "", "", "CLIENTE") { fail() }
        assertTrue(vm.aviso.value.contains("Completa"))

        // Act & Assert: creación válida
        vm.guardarUsuario("ana", "Ana", cliente.correo, "CLIENTE") { exitos++ }
        runCurrent()
        val usuario = vm.usuarios.first().single()

        // Act & Assert: edición
        vm.editarUsuario(usuario, "", cliente.correo, "", "", "CLIENTE") { fail() }
        assertTrue(vm.aviso.value.contains("obligatorios"))

        vm.editarUsuario(usuario, "Alicia", cliente.correo, "1", "Calle", "CLIENTE") { exitos++ }
        runCurrent()
        assertEquals("Alicia", vm.usuarios.first().single().nombre)
        assertEquals(2, exitos)
        assertFalse(vm.guardandoEdicion.value)

        // Act & Assert: eliminación
        api.borrar = { throw IOException() }
        vm.eliminarUsuario(usuario.username)
        runCurrent()
        assertTrue(vm.usuarios.first().isEmpty())
        assertNull(vm.usuarioEliminando.value)
    }

    @Test
    fun categoriasValidanNombreYPermitenCambiarEstado() = runTest {
        // Arrange
        val vm = AdministracionViewModel(repo, datos.comercial)
        runCurrent()

        // Act & Assert: validación
        vm.guardarCategoria("x", "") { fail() }
        assertTrue(vm.aviso.value.contains("2 caracteres"))

        // Act & Assert: creación y cambio de estado
        var guardada = false
        vm.guardarCategoria("Nueva", "Descripción") { guardada = true }
        runCurrent()
        assertTrue(guardada)

        vm.cambiarEstadoCategoria("Nueva", false)
        runCurrent()
        assertFalse(vm.categorias.first().single { it.nombre == "Nueva" }.activa)

        vm.eliminarCategoria("Nueva")
        runCurrent()
        assertEquals(3, vm.categorias.first().size)

        vm.limpiarAviso()
        assertEquals("", vm.aviso.value)
    }

    @Test
    fun operacionesLocalesDeUsuariosDeleganAlDao() = runTest {
        // Arrange
        val comercial = datos.comercial

        // Act
        comercial.guardarUsuario(cliente)
        val totalTrasGuardar = comercial.contarUsuarios()
        comercial.cambiarEstadoUsuario(cliente.username, false)
        val estadoDesactivado = comercial.observarUsuarios().first().single().activo
        comercial.eliminarUsuario(cliente.username)
        val totalFinal = comercial.contarUsuarios()

        // Assert
        assertEquals(1, totalTrasGuardar)
        assertFalse(estadoDesactivado)
        assertEquals(0, totalFinal)
    }
}