package com.example.proyecto_apliacion.funcionalidades.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val preferencias = remember { context.getSharedPreferences("kaiju_perfil", android.content.Context.MODE_PRIVATE) }
    var nombre by remember { mutableStateOf(preferencias.getString("nombre", "Operador Kaiju") ?: "Operador Kaiju") }
    var correo by remember { mutableStateOf(preferencias.getString("correo", "operador@kaiju.local") ?: "operador@kaiju.local") }
    var editando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Mi perfil") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Text(nombre, style = MaterialTheme.typography.headlineSmall)
            Text("Administración de inventario", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            OutlinedTextField(value = nombre, onValueChange = { nombre = it; guardado = false }, label = { Text("Nombre visible") }, enabled = editando, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Badge, null) })
            OutlinedTextField(value = correo, onValueChange = { correo = it; guardado = false }, label = { Text("Correo") }, enabled = editando, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Security, null) })
            if (editando) {
                Button(onClick = { if (nombre.isNotBlank() && correo.contains('@')) { preferencias.edit().putString("nombre", nombre.trim()).putString("correo", correo.trim()).apply(); nombre = nombre.trim(); correo = correo.trim(); editando = false; guardado = true } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar cambios") }
                TextButton(onClick = { editando = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            } else {
                OutlinedButton(onClick = { editando = true; guardado = false }, modifier = Modifier.fillMaxWidth()) { Text("Editar perfil") }
            }
            if (guardado) Text("Perfil actualizado y guardado en el dispositivo", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
            Text("Kaiju Inventory · Aplicación académica", style = MaterialTheme.typography.labelSmall)
        }
    }
}
