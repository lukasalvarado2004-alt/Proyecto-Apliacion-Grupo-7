package com.example.proyecto_apliacion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_apliacion.data.DefaultAppContainer
import com.example.proyecto_apliacion.data.remote.SessionManager
import com.example.proyecto_apliacion.funcionalidades.autenticacion.LoginScreen
import com.example.proyecto_apliacion.funcionalidades.cliente.CatalogoClienteScreen
import com.example.proyecto_apliacion.funcionalidades.comercial.AdministracionScreen
import com.example.proyecto_apliacion.funcionalidades.comercial.VentasScreen
import com.example.proyecto_apliacion.funcionalidades.inventario.InventarioScreen
import com.example.proyecto_apliacion.funcionalidades.movimientos.MovimientosScreen
import com.example.proyecto_apliacion.funcionalidades.panel_inicio.PanelInicioViewModel
import com.example.proyecto_apliacion.funcionalidades.panel_inicio.PanelInicioViewModelFactory
import com.example.proyecto_apliacion.funcionalidades.perfil.PerfilScreen
import com.example.proyecto_apliacion.ui.theme.ProyectoApliacionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProyectoApliacionTheme {
                KaijuApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaijuApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var autenticado by remember { mutableStateOf(false) }
    var pantalla by remember { mutableStateOf("panel") }
    var usuario by remember { mutableStateOf("") }

    val rolBackend = if (autenticado) sessionManager.obtenerRol() else "PUBLICO"
    val rol = when (rolBackend) {
        "ADMINISTRADOR" -> "ADMIN"
        "VENDEDOR" -> "VENTAS"
        "INVENTARIO" -> "INVENTARIO"
        else -> "PUBLICO"
    }

    val cerrarSesionGlobal = {
        sessionManager.cerrarSesion()
        pantalla = "panel"
        usuario = ""
        autenticado = false
    }

    Crossfade(
        targetState = autenticado,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "TransicionSesion"
    ) { estaAutenticado ->
        if (!estaAutenticado) {
            LoginScreen(
                onLoginCorrecto = { u ->
                    usuario = u
                    autenticado = true
                    pantalla = "panel"
                },
                onLoginCliente = { u ->
                    if (u == "Invitado") sessionManager.cerrarSesion()
                    usuario = u
                    autenticado = true
                    pantalla = "catalogo"
                }
            )
        } else {
            val destinos = buildList {
                add("panel" to "Inicio")
                add("catalogo" to "Catálogo")
                if (rol in listOf("ADMIN", "VENTAS")) add("ventas" to "Ventas")
                if (rol in listOf("ADMIN", "INVENTARIO")) {
                    add("inventario" to "Inventario")
                    add("movimientos" to "Movimientos")
                }
                if (rol == "ADMIN") add("admin" to "Administrar")
                add("perfil" to "Perfil")
            }
            if (destinos.none { it.first == pantalla }) pantalla = "panel"
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("KAIJU INVENTORY", style = MaterialTheme.typography.titleMedium)
                                Text("Sesión: $usuario · $rol", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        destinos.forEach { (key, label) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = pantalla == key,
                                onClick = {
                                    pantalla = key
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        when (key) {
                                            "panel" -> Icons.Default.Home
                                            "catalogo" -> Icons.Default.Store
                                            "ventas" -> Icons.Default.PointOfSale
                                            "inventario" -> Icons.Default.Inventory
                                            "movimientos" -> Icons.Default.History
                                            "admin" -> Icons.Default.AdminPanelSettings
                                            else -> Icons.Default.AccountCircle
                                        },
                                        contentDescription = label
                                    )
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(
                            label = { Text("Cerrar sesión") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                cerrarSesionGlobal()
                            },
                            icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(destinos.firstOrNull { it.first == pantalla }?.second ?: "Kaiju Inventory") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Abrir o cerrar menú")
                                }
                            }
                        )
                    }
                ) { contentPadding ->
                    Box(Modifier.fillMaxSize().padding(contentPadding)) {
                        Crossfade(
                            targetState = pantalla,
                            animationSpec = tween(durationMillis = 280),
                            label = "NavegacionPantallas"
                        ) { pantallaActual ->
                            when (pantallaActual) {
                                "catalogo" -> CatalogoClienteScreen(onVolverClick = { pantalla = "panel" })
                                "ventas" -> VentasScreen(usuario)
                                "inventario" -> InventarioScreen(onVolverClick = { pantalla = "panel" })
                                "movimientos" -> MovimientosScreen()
                                "admin" -> AdministracionScreen()
                                "perfil" -> PerfilScreen(onCerrarSesion = cerrarSesionGlobal)
                                else -> PanelInicio(
                                    usuario = usuario,
                                    rol = rol,
                                    onInventario = { pantalla = if (rol == "VENTAS") "ventas" else "inventario" },
                                    onMovimientos = { pantalla = "movimientos" },
                                    onVentas = { pantalla = "ventas" },
                                    onCatalogo = { pantalla = "catalogo" },
                                    onAdmin = { pantalla = "admin" },
                                    onCerrarSesion = cerrarSesionGlobal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelInicio(
    usuario: String,
    rol: String,
    onInventario: () -> Unit,
    onMovimientos: () -> Unit,
    onVentas: () -> Unit,
    onCatalogo: () -> Unit,
    onAdmin: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val container = remember { DefaultAppContainer(context) }

    val viewModel: PanelInicioViewModel = viewModel(
        factory = PanelInicioViewModelFactory(container.productoRepository)
    )
    val resumen by viewModel.resumenState.collectAsState()

    val stockAnimado by animateIntAsState(
        targetValue = resumen.stockTotal,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ContadorStock"
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Kaiju Inventory · $rol") }) }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("¡Hola, $usuario!", style = MaterialTheme.typography.headlineMedium)
                Text("Resumen de inventario y operaciones")
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stock total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$stockAnimado unidades",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Productos con stock bajo (≤ 15): ${resumen.productosBajoStock}",
                            color = if (resumen.productosBajoStock > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item { Button(onClick = onCatalogo, modifier = Modifier.fillMaxWidth()) { Text("Consultar catálogo y disponibilidad") } }
            if (rol in listOf("ADMIN", "INVENTARIO")) item { OutlinedButton(onClick = onInventario, modifier = Modifier.fillMaxWidth()) { Text("Administrar inventario") } }
            if (rol in listOf("ADMIN", "VENTAS")) item { OutlinedButton(onClick = onVentas, modifier = Modifier.fillMaxWidth()) { Text("Registrar venta") } }
            if (rol in listOf("ADMIN", "INVENTARIO")) item { OutlinedButton(onClick = onMovimientos, modifier = Modifier.fillMaxWidth()) { Text("Ver movimientos") } }
            if (rol == "ADMIN") item { OutlinedButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) { Text("Gestionar usuarios y categorías") } }
            item { OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") } }
        }
    }
}