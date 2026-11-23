package com.example.proyecto_apliacion

import com.example.proyecto_apliacion.data.repository.DefaultUsuarioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class UsuariosTest {
    private val dao = UsuariosEnMemoria()
    private val api = ApiUsuariosPrueba()
    private val repo = DefaultUsuarioRepository(dao, api)

    @Test
    fun registroGuardaLaCuentaConfirmadaYPermiteConsultarCredenciales() = runTest {
        // Arrange
        val usuarioNuevo = cliente

        // Act
        val creado = repo.registrarUsuario(usuarioNuevo).getOrThrow()
        val loginValido = repo.login(cliente.correo, cliente.clave)
        val loginInvalido = repo.login(cliente.correo, "incorrecta")

        // Assert
        assertEquals(7L, creado.idRemoto)
        assertEquals(creado, loginValido)
        assertNull(loginInvalido)
        assertEquals(listOf(creado), repo.observarUsuariosLocales().first())
    }

    @Test
    fun errorHttpDeApiRetornaFailureYNoGuardaLocal() = runTest {
        // Arrange
        api.crear = {
            Response.error(409, "Correo duplicado".toResponseBody("text/plain".toMediaTypeOrNull()))
        }

        // Act
        val resultado = repo.registrarUsuario(cliente)

        // Assert
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("409"))
        assertEquals(0, dao.contar())
    }

    @Test
    fun modoOfflineGuardaEnLocalSiNoExisteCorreo() = runTest {
        // Arrange
        api.crear = { throw IOException("sin red") }

        // Act
        val resultado = repo.registrarUsuario(cliente)

        // Assert
        assertTrue(resultado.isSuccess)
        assertEquals(1, dao.contar())
    }

    @Test
    fun asignacionDeRolGuardaElRolConfirmado() = runTest {
        // Arrange
        val adminEntrada = cliente.copy(rol = "ADMIN")

        // Act
        val adminCreado = repo.registrarUsuario(adminEntrada).getOrThrow()

        // Assert
        assertEquals("ADMINISTRADOR", adminCreado.rol)
        assertEquals(adminCreado, dao.buscarPorNombre(adminCreado.username))
    }

    @Test
    fun falloDeRolConservaLaCuentaCreadaComoCliente() = runTest {
        // Arrange
        api.rol = { throw crearHttpException(403, "Acceso denegado") }
        val adminEntrada = cliente.copy(rol = "ADMIN")

        // Act
        val creado = repo.registrarUsuario(adminEntrada).getOrThrow()

        // Assert
        assertEquals("CLIENTE", creado.rol)
        assertEquals("CLIENTE", dao.datos.value.single().rol)
    }

    @Test
    fun editarCuentaSinIdNormalizaElRolYEditarRemotaUsaSuId() = runTest {
        // Arrange
        val usuarioSinId = cliente.copy(rol = "VENTAS")

        // Act
        val local = repo.actualizarUsuario(usuarioSinId).getOrThrow()
        repo.sincronizarUsuariosDesdeAws()
        val editado = repo.actualizarUsuario(cliente.copy(nombre = "Alicia")).getOrThrow()
        val conNuevoRol = repo.actualizarUsuario(editado.copy(rol = "ADMIN")).getOrThrow()

        // Assert
        assertEquals("VENDEDOR", local.rol)
        assertEquals("Alicia", editado.nombre)
        assertEquals(7L, editado.idRemoto)
        assertEquals("ADMINISTRADOR", conNuevoRol.rol)
    }

    @Test
    fun erroresDeEdicionConservanLaCopiaAnterior() = runTest {
        // Arrange
        val inicial = repo.registrarUsuario(cliente).getOrThrow()

        for (codigo in listOf(403, 409, 500)) {
            api.editar = { throw crearHttpException(codigo, "detalle") }

            // Act
            val resultado = repo.actualizarUsuario(inicial.copy(nombre = "Cambio"))

            // Assert
            assertTrue(resultado.isFailure)
            assertTrue(resultado.exceptionOrNull()!!.message!!.contains(codigo.toString()))
            assertEquals(inicial, dao.datos.value.single())
        }
    }

    @Test
    fun eliminarRemotoEliminaCuandoServidorRespondeExito() = runTest {
        // Arrange
        repo.registrarUsuario(cliente).getOrThrow()
        var idEliminado = -1L
        api.borrar = {
            idEliminado = it
            Response.success(Unit)
        }

        // Act
        repo.eliminarUsuario(cliente.username).getOrThrow()

        // Assert
        assertEquals(7L, idEliminado)
        assertEquals(0, dao.contar())
    }

    @Test
    fun eliminaCuentaSoloLocalSinLlamarApi() = runTest {
        // Arrange
        dao.guardar(cliente)
        api.borrar = { _: Long ->
            fail("La cuenta local no tiene ID remoto")
            Response.success(Unit)
        }

        // Act
        repo.eliminarUsuario(cliente.username).getOrThrow()

        // Assert
        assertEquals(0, dao.contar())
    }
}