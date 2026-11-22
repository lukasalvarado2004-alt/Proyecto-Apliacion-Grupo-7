package com.example.proyecto_apliacion.funcionalidades.panel_inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_apliacion.data.DefaultAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelInicioScreen(
    pantallaSeleccionada: String,
    onCambiarPantalla: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { DefaultAppContainer(context) }
    val viewModel: PanelInicioViewModel = viewModel(
        factory = PanelInicioViewModelFactory(container.productoRepository)
    )
    val resumen by viewModel.resumenState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Kaiju Inventory", color = Color.White) },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        bottomBar = {
            MenuInferior(
                pantallaActiva = pantallaSeleccionada,
                onCambiar = onCambiarPantalla
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF0D1B2A))
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(text = "¡Hola, Operador!", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(text = "Resumen general del Inventario", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF4F6F9),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tarjeta de Stock Total dinámico
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clickable { onCambiarPantalla("inventario") },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Inventory, contentDescription = null, tint = Color(0xFF52B788))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stock total", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("${resumen.stockTotal}", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF0D1B2A))
                                Text("unidades", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        // Tarjeta de Alertas dinámica
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clickable { onCambiarPantalla("inventario") },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD90429))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Productos bajo", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("${resumen.productosBajoStock}", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFD90429))
                                Text("productos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clickable { onCambiarPantalla("ventas") },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ShowChart, contentDescription = "Ventas", tint = Color(0xFF4A90E2), modifier = Modifier.size(32.dp))
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clickable { onCambiarPantalla("movimientos") },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = "Movimientos", tint = Color(0xFF4A90E2), modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuInferior(
    pantallaActiva: String,
    onCambiar: (String) -> Unit
) {
    NavigationBar(containerColor = Color(0xFF0D1B2A)) {
        NavigationBarItem(
            selected = pantallaActiva == "panel",
            onClick = { onCambiar("panel") },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF52B788),
                selectedTextColor = Color(0xFF52B788),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                indicatorColor = Color(0xFF1B2A4A)
            )
        )
        NavigationBarItem(
            selected = pantallaActiva == "inventario",
            onClick = { onCambiar("inventario") },
            icon = { Icon(Icons.Filled.Inventory, contentDescription = null) },
            label = { Text("Inventario") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF52B788),
                selectedTextColor = Color(0xFF52B788),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                indicatorColor = Color(0xFF1B2A4A)
            )
        )
        NavigationBarItem(
            selected = pantallaActiva == "movimientos",
            onClick = { onCambiar("movimientos") },
            icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
            label = { Text("Movimientos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF52B788),
                selectedTextColor = Color(0xFF52B788),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                indicatorColor = Color(0xFF1B2A4A)
            )
        )
        NavigationBarItem(
            selected = pantallaActiva == "perfil",
            onClick = { onCambiar("perfil") },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Perfil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF52B788),
                selectedTextColor = Color(0xFF52B788),
                unselectedIconColor = Color.White,
                unselectedTextColor = Color.White,
                indicatorColor = Color(0xFF1B2A4A)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PanelInicioScreenPreview() {
    PanelInicioScreen(pantallaSeleccionada = "panel", onCambiarPantalla = {})
}