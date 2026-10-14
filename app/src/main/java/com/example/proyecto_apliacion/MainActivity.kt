package com.example.proyecto_apliacion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.ProductoRepository
import com.example.proyecto_apliacion.funcionalidades.autenticacion.LoginScreen
import com.example.proyecto_apliacion.funcionalidades.inventario.InventarioScreen
import com.example.proyecto_apliacion.funcionalidades.movimientos.MovimientosScreen
import com.example.proyecto_apliacion.funcionalidades.perfil.PerfilScreen
import com.example.proyecto_apliacion.ui.theme.ProyectoApliacionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ProyectoApliacionTheme { KaijuApp() } }
    }
}

@Composable
private fun KaijuApp() {
    var autenticado by remember { mutableStateOf(false) }
    var pantalla by remember { mutableStateOf("panel") }
    val context = LocalContext.current
    if (!autenticado) {
        LoginScreen(onLoginCorrecto = { autenticado = true })
    } else {
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = pantalla == "panel", onClick = { pantalla = "panel" }, icon = { Icon(Icons.Default.Home, "Inicio") }, label = { Text("Inicio") })
                NavigationBarItem(selected = pantalla == "inventario", onClick = { pantalla = "inventario" }, icon = { Icon(Icons.Default.Inventory, "Inventario") }, label = { Text("Inventario") })
                NavigationBarItem(selected = pantalla == "movimientos", onClick = { pantalla = "movimientos" }, icon = { Icon(Icons.Default.History, "Movimientos") }, label = { Text("Movimientos") })
                NavigationBarItem(selected = pantalla == "perfil", onClick = { pantalla = "perfil" }, icon = { Icon(Icons.Default.AccountCircle, "Perfil") }, label = { Text("Perfil") })
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (pantalla) {
                    "inventario" -> InventarioScreen(onVolverClick = { pantalla = "panel" })
                    "movimientos" -> MovimientosScreen()
                    "perfil" -> PerfilScreen(onCerrarSesion = { pantalla = "panel"; autenticado = false })
                    else -> PanelInicio(onInventario = { pantalla = "inventario" }, onMovimientos = { pantalla = "movimientos" }, onCerrarSesion = { pantalla = "panel"; autenticado = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelInicio(onInventario: () -> Unit, onMovimientos: () -> Unit, onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val db = remember { KaijuDatabase.obtener(context) }
    val repository = remember { ProductoRepository(db.productoDao(), db.movimientoDao()) }
    val productos by repository.observarProductos().collectAsState(initial = emptyList())
    LaunchedEffect(repository) { repository.inicializarSiVacio() }
    Scaffold(topBar = { TopAppBar(title = { Text("Kaiju Inventory") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("¡Hola, Operador!", style = MaterialTheme.typography.headlineMedium)
                Text("Resumen de tu inventario", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stock registrado", style = MaterialTheme.typography.titleMedium)
                    Text("${productos.sumOf { it.stock }} unidades", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${productos.size} productos registrados")
                    Button(onClick = onInventario, modifier = Modifier.fillMaxWidth()) { Text("Administrar inventario") }
                    OutlinedButton(onClick = onMovimientos, modifier = Modifier.fillMaxWidth()) { Text("Ver movimientos") }
                } }
            }
            item { OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") } }
        }
    }
}
