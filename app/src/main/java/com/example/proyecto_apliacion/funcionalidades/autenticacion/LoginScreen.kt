package com.example.proyecto_apliacion.funcionalidades.autenticacion

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginCorrecto: () -> Unit,
    onLoginCliente: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    var mostrandoRegistro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.loginExitoso) {
        if (viewModel.loginExitoso) {
            val esCliente = viewModel.esIngresoCliente
            viewModel.consumirLoginExitoso()
            if (esCliente) {
                onLoginCliente()
            } else {
                onLoginCorrecto()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mostrandoRegistro) "Kaiju - Registro de Cliente" else "Kaiju Inventory - Acceso"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(all = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!mostrandoRegistro) {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = viewModel.usuario,
                    onValueChange = { viewModel.onUsuarioCambiado(it) },
                    label = { Text("Usuario o Correo") },
                    isError = viewModel.mensajeError != null && viewModel.usuario.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.contrasena,
                    onValueChange = { viewModel.onContrasenaCambiada(it) },
                    label = { Text("Contraseña") },
                    isError = viewModel.mensajeError != null && viewModel.contrasena.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                viewModel.mensajeError?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { viewModel.iniciarSesion(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Ingresar")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLoginCliente,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Ver Catálogo como Invitado")
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { mostrandoRegistro = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "¿No tienes cuenta? Regístrate aquí")
                }
            } else {
                FormularioRegistroVista(
                    viewModel = viewModel,
                    onRegistroExitoso = { correo ->
                        viewModel.onUsuarioCambiado(correo)
                        mostrandoRegistro = false
                    },
                    onVolverAlLogin = {
                        mostrandoRegistro = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FormularioRegistroVista(
    viewModel: LoginViewModel,
    onRegistroExitoso: (correo: String) -> Unit,
    onVolverAlLogin: () -> Unit
) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmarClave by rememberSaveable { mutableStateOf("") }
    var mensajeError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it; mensajeError = null },
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it; mensajeError = null },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it; mensajeError = null },
            label = { Text("Contraseña (mínimo 4 caracteres)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmarClave,
            onValueChange = { confirmarClave = it; mensajeError = null },
            label = { Text("Validar Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        mensajeError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombre.isBlank() || correo.isBlank() || clave.isBlank() || confirmarClave.isBlank()) {
                    mensajeError = "Por favor completa todos los campos."
                } else if (!correo.contains("@")) {
                    mensajeError = "Ingresa un correo electrónico válido."
                } else if (clave.length < 4) {
                    mensajeError = "La contraseña debe tener al menos 4 caracteres."
                } else if (clave != confirmarClave) {
                    mensajeError = "Las contraseñas no coinciden."
                } else {
                    viewModel.registrarUsuario(
                        context = context,
                        nombre = nombre,
                        correo = correo,
                        clave = clave,
                        onExito = {
                            Toast.makeText(context, "¡Cuenta creada con éxito en la BD!", Toast.LENGTH_LONG).show()
                            onRegistroExitoso(correo.trim().lowercase())
                        },
                        onError = { err ->
                            mensajeError = err
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Registrarme")
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onVolverAlLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Ya tengo cuenta · Volver al inicio de sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginCorrecto = {}, onLoginCliente = {})
}