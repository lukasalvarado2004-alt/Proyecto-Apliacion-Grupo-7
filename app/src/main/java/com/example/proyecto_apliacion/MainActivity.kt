package com.example.proyecto_apliacion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.proyecto_apliacion.funcionalidades.comercial.*
import com.example.proyecto_apliacion.ui.theme.ProyectoApliacionTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { ProyectoApliacionTheme { KaijuApp() } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaijuApp() {
    var autenticado by remember { mutableStateOf(false) }
    var pantalla by remember { mutableStateOf("panel") }
    var usuario by remember { mutableStateOf("") }
    val rol = when(usuario) { "admin" -> "ADMIN"; "ventas" -> "VENTAS"; "inventario" -> "INVENTARIO"; else -> "PUBLICO" }
    if (!autenticado) {
        LoginScreen(
            onLoginCorrecto = { u -> usuario = u; autenticado = true; pantalla = "panel" },
            onLoginCliente = { u -> usuario = u; autenticado = true; pantalla = "catalogo" }
        )
    } else {
        val destinos = buildList {
            add("panel" to "Inicio")
            add("catalogo" to "Catálogo")
            if (rol in listOf("ADMIN", "VENTAS")) add("ventas" to "Ventas")
            if (rol in listOf("ADMIN", "INVENTARIO")) { add("inventario" to "Inventario"); add("movimientos" to "Movimientos") }
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
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            icon = { Icon(when (key) {
                                "panel" -> Icons.Default.Home
                                "catalogo" -> Icons.Default.Store
                                "ventas" -> Icons.Default.PointOfSale
                                "inventario" -> Icons.Default.Inventory
                                "movimientos" -> Icons.Default.History
                                "admin" -> Icons.Default.AdminPanelSettings
                                else -> Icons.Default.AccountCircle
                            }, contentDescription = label) },
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
                            pantalla = "panel"
                            usuario = ""
                            autenticado = false
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
                            IconButton(onClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir o cerrar menú")
                            }
                        }
                    )
                }
            ) { contentPadding ->
                Box(Modifier.fillMaxSize().padding(contentPadding)) {
                    when(pantalla) {
                        "catalogo" -> CatalogoPublicoScreen()
                        "ventas" -> VentasScreen(usuario)
                        "inventario" -> InventarioScreen(onVolverClick={pantalla="panel"})
                        "movimientos" -> MovimientosScreen()
                        "admin" -> AdministracionScreen()
                        "perfil" -> PerfilScreen(onCerrarSesion={pantalla="panel";usuario="";autenticado=false})
                        else -> PanelInicio(usuario, rol, onInventario={pantalla=if(rol=="VENTAS")"ventas" else "inventario"}, onMovimientos={pantalla="movimientos"}, onVentas={pantalla="ventas"}, onCatalogo={pantalla="catalogo"}, onAdmin={pantalla="admin"}, onCerrarSesion={pantalla="panel";usuario="";autenticado=false})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelInicio(usuario:String, rol:String, onInventario:()->Unit, onMovimientos:()->Unit, onVentas:()->Unit, onCatalogo:()->Unit, onAdmin:()->Unit, onCerrarSesion:()->Unit) {
    val context= LocalContext.current; val db= remember { KaijuDatabase.obtener(context) }; val repository= remember { ProductoRepository(db.productoDao(),db.movimientoDao()) }
    val productos by repository.observarProductos().collectAsState(initial= emptyList()); LaunchedEffect(repository){repository.inicializarSiVacio()}
    Scaffold(topBar={TopAppBar(title={Text("Kaiju Inventory · $rol")})}) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("¡Hola, $usuario!",style=MaterialTheme.typography.headlineMedium); Text("Resumen de inventario y operaciones") }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Stock total",style=MaterialTheme.typography.titleMedium); Text("${productos.sumOf{it.stock}} unidades",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary); Text("${productos.size} productos registrados"); Text("Productos con stock bajo (≤ 10): ${productos.count{it.stock<=10}}",color=if(productos.any{it.stock<=10})MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } } }
        item { Button(onClick=onCatalogo,modifier=Modifier.fillMaxWidth()){Text("Consultar catálogo y disponibilidad")} }
        if(rol in listOf("ADMIN","INVENTARIO")) item { OutlinedButton(onClick=onInventario,modifier=Modifier.fillMaxWidth()){Text("Administrar inventario")} }
        if(rol in listOf("ADMIN","VENTAS")) item { OutlinedButton(onClick=onVentas,modifier=Modifier.fillMaxWidth()){Text("Registrar venta") } }
        if(rol in listOf("ADMIN","INVENTARIO")) item { OutlinedButton(onClick=onMovimientos,modifier=Modifier.fillMaxWidth()){Text("Ver movimientos") } }
        if(rol=="ADMIN") item { OutlinedButton(onClick=onAdmin,modifier=Modifier.fillMaxWidth()){Text("Gestionar usuarios y categorías") } }
        item { OutlinedButton(onClick=onCerrarSesion,modifier=Modifier.fillMaxWidth()){Text("Cerrar sesión") } }
    } }
}
