# Kaiju Inventory — Equipo 7


Aplicación Android del Equipo 7 para administrar un inventario y registrar ventas en pesos chilenos. Un visitante puede consultar el catálogo. El personal de ventas registra operaciones, el encargado de inventario mantiene productos y movimientos, y el administrador también gestiona usuarios y categorías.

El proyecto usa Kotlin, Jetpack Compose y MVVM. Room conserva los datos comerciales en el dispositivo. Retrofit conecta la app con el backend de usuarios desplegado en AWS y con una API externa de tipos de cambio. Esta guía explica cómo ejecutar la aplicación y seguir una operación desde la pantalla hasta los datos y sus pruebas.

## Índice

1. [Requisitos](#requisitos)
2. [Ejecutar el proyecto](#ejecutar-el-proyecto)
3. [Arquitectura](#arquitectura)
4. [Replicar el proyecto paso a paso](#replicar-el-proyecto-paso-a-paso)
5. [Testear con piezas falsas](#testear-con-piezas-falsas)
6. [Comandos útiles](#comandos-útiles)

## Requisitos

| Componente | Configuración del proyecto |
|---|---|
| Android Studio | Versión compatible con Android Gradle Plugin 9.4.1. |
| Gradle | Usar el wrapper incluido, versión 9.6.0. |
| Java | Usar el JDK integrado de Android Studio compatible con el wrapper. El módulo configura el código Java con destino 11. |
| Android SDK | Plataforma 37 para compilar. El dispositivo o emulador debe usar API 24 o superior. |
| Conexión | Necesaria en la primera sincronización para descargar dependencias, y para probar el backend remoto y las divisas. |

## Ejecutar el proyecto

1. Descomprimir el proyecto. En Android Studio, elegir **Open** y abrir la carpeta que contiene `settings.gradle.kts` y `gradlew.bat`.
2. Instalar la plataforma SDK 37 desde **SDK Manager** si falta. Sincronizar Gradle.
3. Iniciar un emulador o conectar un teléfono con depuración USB. Seleccionar el dispositivo y ejecutar **app**.
4. En la pantalla de acceso, elegir **Invitado** para ver el catálogo, usar un operador de demostración o iniciar sesión con el correo de una cuenta remota.

| Acceso de demostración | Clave | Recorrido sugerido |
|---|---|---|
| `admin` | `1234` | Abrir administración de usuarios y categorías. |
| `ventas` | `5678` | Registrar una venta y consultar su total. |
| `inventario` | `0000` | Crear un producto y consultar su movimiento de alta. |

Estas tres cuentas se validan en `LoginViewModel` mediante la rama de demostración local. Para comprobar la integración con AWS, usar una cuenta remota registrada por correo. La implementación del servidor se despliega por separado del módulo Android.

**Recorrido de comprobación.** Crear un producto con 5 unidades a $100 desde Inventario. Entrar como vendedor, registrar 2 unidades y comprobar un total de $200. Al volver al inventario deben quedar 3 unidades. En Movimientos se puede revisar la salida asociada. Para verificar por separado el registro remoto, crear una cuenta de prueba y consultar su ID en el servidor.

## Arquitectura

En inventario, la pantalla envía eventos al ViewModel. Este valida los datos, llama al repositorio y publica el estado que Compose observa. El recorrido de una cuenta agrega una segunda fuente de datos, porque el repositorio de usuarios coordina la API y la copia local.

| Paquete o archivo | Responsabilidad | Ejemplo |
|---|---|---|
| `funcionalidades/` | Pantallas y lógica agrupadas por tarea. | `InventarioScreen`, `InventarioViewModel`, `VentasViewModel`. |
| `data/repository/` | Operaciones de inventario, comercio y usuarios. | `DefaultUsuarioRepository` registra una cuenta y procesa la respuesta. |
| `data/local/` | Entidades, DAO y configuración de Room. | `ProductoDao` consulta productos y descuenta stock. |
| `data/remote/` | Contratos HTTP y gestión de la sesión. | `UsuarioApiService`, `IndicadoresApiService`, `SessionManager`. |
| `data/dto/` | Formato de entrada y salida de la API y conversión a entidades locales. | `UsuarioEntradaDTO`, `UsuarioSalidaDTO`, `UsuarioMapper`. |
| `data/AppContainer.kt` | Construcción de clientes y repositorios. | `DefaultAppContainer`. |
| `MainActivity.kt` | Entrada, sesión y navegación por rol. | Acceso al catálogo, ventas, inventario y administración. |

Los archivos Kotlin de producción se encuentran en `app/src/main/java/com/example/proyecto_apliacion/`. El nombre del paquete conserva la ortografía original del proyecto.

## Replicar el proyecto paso a paso

### Paso 1. Crear el proyecto

Partir de una actividad Android con Kotlin y Compose. Usar el identificador `com.example.proyecto_apliacion`, SDK mínimo 24 y SDK de compilación y destino 37. Para reproducir el proyecto entregado, tomar las versiones de `gradle/libs.versions.toml` y del módulo `app/build.gradle.kts`.

### Paso 2. Añadir las dependencias

| Dependencia | Uso en Kaiju Inventory |
|---|---|
| Compose y Material 3 | Catálogo, formularios, paneles y navegación visible por rol. |
| Lifecycle ViewModel y corrutinas | Estado de pantalla y trabajo asíncrono. |
| Room 2.8.5 y KSP | Persistencia SQLite y generación de implementaciones de DAO. |
| Retrofit 2.9.0 y converter-gson 2.9.0 | Peticiones HTTP y traducción entre JSON y DTO. |
| Coil Compose 2.7.0 | Presentación de imágenes de productos. |
| JUnit 4.13.2, MockK 1.13.10 y coroutines-test 1.8.1 | Pruebas unitarias, dobles de prueba y control de corrutinas. |

Gradle configura JUnit Platform y el motor Vintage para descubrir las pruebas escritas con `org.junit.Test`. También declara Kotest y JUnit Jupiter. Las tres clases de negocio descritas más abajo usan JUnit 4.

### Paso 3. Configurar el manifiesto

En `app/src/main/AndroidManifest.xml` se declara `android.permission.INTERNET` y se registra `.MainActivity` como actividad de inicio. La cámara se declara opcional. El manifiesto habilita `usesCleartextTraffic` para que Android pueda acceder al backend HTTP configurado. El cliente externo de divisas utiliza HTTPS.

### Paso 4. Definir los modelos de datos

Room abre `kaiju_inventory.db`, versión 4, con seis entidades: `Producto`, `Movimiento`, `Usuario`, `Categoria`, `Venta` y `DetalleVenta`. El detalle conserva el precio unitario de la operación.

En la red, `LoginDTO` transporta las credenciales y `TokenDTO` recibe el token y el rol. Los modelos de usuario distinguen los nombres que emplea el servidor: por ejemplo, una cuenta se envía con `correo` y la respuesta usa `email`. El mapper transforma la respuesta a la entidad local y conserva el identificador del servidor en `idRemoto`.

### Paso 5. Configurar Retrofit y seguir una petición

#### 5.1. Declarar las interfaces

Las anotaciones describen el método HTTP, la ruta y el cuerpo. Por ejemplo, la interfaz de divisas tiene una función suspendida:

```kotlin
interface IndicadoresApiService {
    @GET("v6/latest/USD")
    suspend fun obtenerTasasCambio(): ExchangeRateResponse
}
```

| Operación del cliente | Ruta declarada | Finalidad |
|---|---|---|
| POST | `api/v1/auth/login` | Enviar credenciales y recibir sesión. |
| GET | `api/v1/auth/me` | Consultar el perfil autenticado. |
| POST / GET | `api/v1/usuarios` | Crear una cuenta o listar usuarios. |
| GET / PUT / DELETE | `api/v1/usuarios/{id}` | Consultar, editar o eliminar un usuario por ID. |
| GET | `api/v1/usuarios/email/{email}` | Buscar por correo. |
| PATCH | `api/v1/usuarios/{id}/rol` | Cambiar el rol de una cuenta. |
| GET | `v6/latest/USD` | Obtener tasas externas con el dólar como base. |

#### 5.2. Construir los clientes

`DefaultAppContainer`, en `AppContainer.kt`, configura dos direcciones base. Para usuarios utiliza `http://52.201.137.48:8081/`, con un cliente OkHttp que incorpora `AuthInterceptor` y `AuthAuthenticator`. Para indicadores utiliza `https://open.er-api.com/` con un cliente separado. Ambos convierten JSON con `GsonConverterFactory`. El árbol también contiene `RetrofitInstance.kt`. Para seguir las llamadas del login y de Inventario, empezar por el contenedor que usan esas pantallas.

#### 5.3. Seguir una llamada de principio a fin

Al entrar en Inventario, la pantalla solicita las tasas. Si el proveedor devuelve una tasa CLP de 900, un producto de $9.000 CLP se muestra a US$10 al activar USD. La fórmula es `precioCLP / tasas["CLP"]`. Esta conversión cambia la presentación del precio. La venta continúa registrándose en CLP.

#### 5.4. Distinguir errores HTTP y fallos de red

Un registro rechazado con 409 debe comunicar el conflicto y dejar el DAO sin la nueva cuenta. Si ocurre una excepción de conexión, el repositorio contempla un respaldo local para un correo nuevo. Por eso, para verificar una escritura remota hay que consultar el ID desde el servidor después del registro. La aparición del usuario en el teléfono también puede deberse al respaldo en Room.

### Paso 6. Implementar los repositorios

`ProductoRepository` coordina productos e historial de movimientos. `ComercialRepository` ofrece operaciones sobre los datos comerciales. `DefaultUsuarioRepository` recibe el DAO y la interfaz HTTP, convierte modelos y procesa el resultado de las operaciones remotas. Esta separación permite probar el mismo repositorio con un DAO en memoria y una API que devuelve respuestas controladas.

### Paso 7. Representar el estado de la interfaz

`InventarioViewModel` combina productos, búsqueda y categoría en `uiState`. La pantalla puede actualizar la lista al cambiar un filtro o recibir datos nuevos desde Room. En el panel de inicio, `PanelInicioViewModel` suma las unidades y cuenta los productos cuyo stock es menor o igual a 15. En la pantalla de divisas también se distinguen carga, datos disponibles y error de conexión.

### Paso 8. Aplicar las reglas en el ViewModel

En `VentasViewModel.registrarVenta` se valida la cantidad solicitada. La operación de venta consulta el producto vigente dentro de una transacción Room, descuenta existencias y calcula el total con el precio actual. El DAO devuelve cuántas filas pudo actualizar. Si el descuento no se realiza, la operación se interrumpe. El precio y el stock que quedaron en la pantalla al seleccionar un producto pueden haber cambiado antes de confirmar.

### Paso 9. Construir las pantallas con Compose

`LoginScreen` permite acceder y registrar una cuenta. `CatalogoClienteScreen` muestra el catálogo. `InventarioScreen` ofrece búsqueda, filtros, edición, fotografía y precios en CLP o USD. En el área comercial se registran ventas y se administran cuentas y categorías. El historial de movimientos permite revisar qué ocurrió después de un alta, una salida o una modificación.

### Paso 10. Conectar el inicio y la navegación

`MainActivity` es el punto de entrada. Usa el estado de sesión y traduce los roles recibidos, como `ADMINISTRADOR` y `VENDEDOR`, a los que usa la navegación. El vendedor accede a ventas, el encargado de inventario a inventario y movimientos, y el administrador a ambas áreas y a administración. El invitado puede abrir el catálogo desde el acceso público.

## Testear con piezas falsas

Los casos se encuentran en `app/src/test/java/com/example/proyecto_apliacion/`. Se organizan con **Arrange, Act y Assert**: preparar entradas y dependencias, ejecutar la operación y comprobar sus efectos.

| Clase | Casos de lógica | Ejemplos |
|---|---:|---|
| `InventarioVentasTest` | 8 | Movimientos, stock vigente, ventas inválidas, filtros, validaciones y resumen del panel. |
| `SesionAdministracionTest` | 7 | Login demo y remoto, token y rol, alta y edición de usuarios, categorías. |
| `UsuariosTest` | 9 | Conflicto HTTP, registro offline, asignación de rol y edición o eliminación por ID. |
| **Total** | **24** | Además existe el ejemplo local `addition_isCorrect`. |

**Ejemplo real.** En `dosVentasConLaMismaSeleccionUsanPrecioYStockVigentes` se prepara un bolso con 5 unidades a $100 y una selección antigua con 20 unidades a $1. El ViewModel registra dos ventas de una unidad. La prueba espera 3 unidades restantes, dos ventas de $100 y un total diario de $200. Así detectaría tanto un descuento de stock incorrecto como un cobro al precio antiguo.

**Qué se sustituye.** Los DAO de prueba almacenan datos en memoria. `ApiUsuariosPrueba` permite devolver una cuenta, un error HTTP o una excepción de conexión. MockK simula dependencias Android y permite verificar interacciones, como guardar el token. `MainRule`, `runTest` y `runCurrent()` controlan la ejecución de corrutinas. En las pruebas de ventas el bloque transaccional se ejecuta con dobles de Room. La atomicidad real de SQLite requiere una comprobación sobre la base real.

### Pruebas instrumentadas y cobertura

En `app/src/androidTest/`, `ExampleInstrumentedTest.useAppContext` comprueba el nombre del paquete en un dispositivo o emulador. Para conocer el porcentaje de lógica cubierta, ejecutar la suite con cobertura y revisar las clases de ViewModel y Repository. Los 24 casos son un conteo de métodos de prueba del código fuente.

## Comandos útiles

Desde la raíz del proyecto, en PowerShell:

```powershell
# Construir el APK de depuración
.\gradlew.bat :app:assembleDebug

# Instalarlo en el emulador o dispositivo conectado
.\gradlew.bat :app:installDebug

# Ejecutar las pruebas unitarias locales y regenerar el informe
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks

# Ejecutar solo las pruebas de inventario y ventas
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.proyecto_apliacion.InventarioVentasTest"

# Ejecutar las pruebas instrumentadas con un dispositivo conectado
.\gradlew.bat :app:connectedDebugAndroidTest
```
