package com.example.proyecto_apliacion.funcionalidades.autenticacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginCorrecto: () -> Unit,
    modifier: Modifier = Modifier,
    // CORREGIDO: Usamos remember para crear el ViewModel sin depender de librerías externas de importación
    viewModel: LoginViewModel = viewModel()
) {
    // Si la validación lógica del ViewModel pasa a verdadero, ejecuta el cambio de pantalla
    LaunchedEffect(viewModel.loginExitoso) {
        if (viewModel.loginExitoso) {
            viewModel.consumirLoginExitoso()
            onLoginCorrecto()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Kaiju Inventory - Control de Acceso") },
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
                .padding(all = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ingreso de Operador",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Entrada de texto: Nombre de usuario
            OutlinedTextField(
                value = viewModel.usuario,
                onValueChange = { viewModel.onUsuarioCambiado(it) },
                label = { Text("Usuario (admin, ventas o inventario)") },
                isError = viewModel.mensajeError != null && viewModel.usuario.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Entrada de texto: Contraseña oculta
            OutlinedTextField(
                value = viewModel.contrasena,
                onValueChange = { viewModel.onContrasenaCambiada(it) },
                label = { Text("Contraseña de Acceso") },
                isError = viewModel.mensajeError != null && viewModel.contrasena.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Renderizado condicional del mensaje de error
            viewModel.mensajeError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón que gatilla la acción en el ViewModel
            Button(
                onClick = { viewModel.iniciarSesion() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Validar Credenciales")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginCorrecto = {})
}
