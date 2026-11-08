# Compartición entre móviles: cómo funciona por dentro

Explicación paso a paso de lo que pasa entre dos móviles cuando uno **transmite** y otro
**recibe**, capa a capa: Bluetooth LE, Wi‑Fi Direct, IP/TCP y el protocolo propio de Cinco
Pelao. Describe la versión 1 del protocolo (`ShareProtocol.VERSION = 1`).

## Índice

1. [Visión general](#1-visión-general)
2. [Las capas](#2-las-capas)
3. [Paso a paso](#3-paso-a-paso)
   - [Paso 0 · Permisos y radios](#paso-0--permisos-y-radios)
   - [Paso 1 · El receptor crea un grupo Wi‑Fi Direct](#paso-1--el-receptor-crea-un-grupo-wifi-direct)
   - [Paso 2 · El receptor abre el puerto TCP](#paso-2--el-receptor-abre-el-puerto-tcp)
   - [Paso 3 · El receptor se anuncia por Bluetooth LE](#paso-3--el-receptor-se-anuncia-por-bluetooth-le)
   - [Paso 4 · El emisor escanea](#paso-4--el-emisor-escanea)
   - [Paso 5 · El emisor lee la baliza GATT](#paso-5--el-emisor-lee-la-baliza-gatt)
   - [Paso 6 · El emisor se une al grupo Wi‑Fi Direct](#paso-6--el-emisor-se-une-al-grupo-wifi-direct)
   - [Paso 7 · Conexión TCP](#paso-7--conexión-tcp)
   - [Paso 8 · Saludo (HELLO) y código de emparejamiento](#paso-8--saludo-hello-y-código-de-emparejamiento)
   - [Paso 9 · Petición (REQUEST) y aprobación](#paso-9--petición-request-y-aprobación)
   - [Paso 10 · Datos (PAYLOAD) y confirmación (ACK)](#paso-10--datos-payload-y-confirmación-ack)
   - [Paso 11 · Cierre](#paso-11--cierre)
   - [Paso 12 · Importación en el receptor](#paso-12--importación-en-el-receptor)
4. [Formato de las tramas](#4-formato-de-las-tramas)
5. [Mensajes](#5-mensajes)
6. [Cancelación, errores y tiempos límite](#6-cancelación-errores-y-tiempos-límite)
7. [Seguridad: qué protege y qué no](#7-seguridad-qué-protege-y-qué-no)
8. [Permisos por versión de Android](#8-permisos-por-versión-de-android)
9. [Qué enseña el panel de detalles técnicos](#9-qué-enseña-el-panel-de-detalles-técnicos)
10. [Mapa del código](#10-mapa-del-código)

---

## 1. Visión general

Hacen falta dos radios porque cada una sirve para una cosa:

- **Bluetooth LE** sirve para **encontrarse**. Gasta poco, descubre en menos de un segundo y no
  necesita conexión previa. Pero es lento: decenas de KB/s en el mejor caso, poco para un
  horario en PNG.
- **Wi‑Fi Direct** sirve para **mover los datos**. Da decenas de MB/s, pero descubrir y conectar
  a pelo es lento y en muchos móviles obliga a aceptar una invitación.

La idea es usar BLE solo para pasar los datos de acceso a la red Wi‑Fi Direct (nombre y
contraseña), y Wi‑Fi Direct + TCP para todo lo demás.

```
 RECIBIR (receptor)                                         TRANSMITIR (emisor)
 ──────────────────                                         ───────────────────
 1  crea grupo Wi‑Fi Direct "DIRECT-xy-CincoPelao"
    y es su dueño (GO) en 192.168.49.1
 2  abre ServerSocket en :8988
 3  anuncio BLE: UUID de servicio + nombre visible ·)))
    servidor GATT con la "baliza"                    ·)))   4  escanea BLE filtrando por el UUID
                                                               y lista receptores (RSSI, distancia)
                         ◄──── conexión GATT ─────────────  5  el usuario elige → MTU 517 →
                         ───── baliza (≈125 B) ──────────►     lee {red, contraseña, puerto}
                                                               y cierra el GATT
                         ◄──── WPA2 + DHCP ───────────────  6  se une al grupo con red + contraseña
                                                               (sin invitación) → 192.168.49.x
 7  accept() ◄──────────────── TCP SYN ───────────────────     connect 192.168.49.1:8988
    deja de anunciarse y cierra el puerto
 8  ◄──────────────────────── HELLO ──────────────────────
    ───────────────────────── HELLO ──────────────────────►
    (los dos calculan el mismo código de 4 cifras)
 9  el usuario elige qué quiere
    ───────────────────────── REQUEST ────────────────────►    el usuario acepta (o ERROR si rechaza)
10  ◄──────────────────────── PAYLOAD (JSON + PNG) ───────     (progreso por bloques de 16 KB)
    ───────────────────────── ACK(sha256) ────────────────►    compara la suma
11  cierra socket y deshace el grupo                           cierra socket y sale del grupo
12  el usuario pone nombre → instancia nueva activa
```

## 2. Las capas

| Capa | Qué hace aquí | Dónde está |
|---|---|---|
| Bluetooth LE: anuncio | Decir "estoy aquí, me llamo X, hablo el protocolo v1" | `BleTransport.advertise` / `scan` |
| Bluetooth LE: GATT | Pasar la baliza con la red y la contraseña | `BleTransport.advertise` / `readBeacon` |
| Wi‑Fi Direct (802.11 + WPA2) | Enlace inalámbrico cifrado entre los dos | `WifiDirectTransport` |
| IPv4 (DHCP del dueño del grupo) | Direcciones 192.168.49.x | Android |
| TCP, puerto 8988 | Flujo fiable y ordenado de bytes | `WifiDirectTransport.openServer/accept/connect` |
| Tramas "5PLO" | Cortar el flujo en mensajes con versión, tamaño y CRC32 | `FrameCodec` |
| Mensajes | HELLO, REQUEST, PAYLOAD, ACK y ERROR | `ShareMessage` |
| Sesión | Orden de los mensajes, código y comprobación SHA-256 | `SenderSession` / `ReceiverSession` |
| Pantallas | Fases, aprobación del usuario e importación | `TransmitViewModel` / `ReceiveViewModel` |

Las capas de tramas, mensajes y sesión son **Kotlin puro**, sin Android, y tienen tests que
mandan bytes reales por sockets TCP dentro del ordenador. Las de radio solo se pueden probar en
un móvil.

---

## 3. Paso a paso

### Paso 0 · Permisos y radios

Al abrir cualquiera de las dos pantallas, `ShareEnvironment.setupIssues` comprueba:

1. Que el móvil tenga **Bluetooth LE y Wi‑Fi Direct** (`FEATURE_BLUETOOTH_LE`, `FEATURE_WIFI_DIRECT`).
2. Los **permisos** de tiempo de ejecución que tocan según la versión y el papel (tabla en la
   [sección 8](#8-permisos-por-versión-de-android)). Se piden solo los que faltan.
3. Que el **Bluetooth** y el **Wi‑Fi** estén encendidos. Wi‑Fi Direct necesita el Wi‑Fi
   encendido, aunque no haga falta estar conectado a ninguna red.
4. Hasta Android 12L, que la **ubicación del sistema** esté activada: Wi‑Fi Direct no funciona
   sin ella en esas versiones.

Si falta algo, la fase es `NeedsSetup` y la pantalla enseña qué arreglar. Al volver a la app
(`onResume`) se comprueba otra vez.

### Paso 1 · El receptor crea un grupo Wi‑Fi Direct

`ReceiveViewModel.runSession` → `WifiDirectTransport.createGroup`.

1. **Credenciales nuevas en cada sesión** (`GroupCredentials.generate`, con `SecureRandom`):
   - Nombre de red (SSID): `DIRECT-` + 2 caracteres al azar + `-CincoPelao`, por ejemplo
     `DIRECT-UY-CincoPelao`. Android exige el prefijo `DIRECT-xy` y como mucho 32 bytes.
   - Contraseña: 16 caracteres de un alfabeto de 56 (sin `0/O`, `1/l/I`), es decir,
     16 · log₂ 56 ≈ **93 bits** de entropía.
2. Se deshace cualquier grupo anterior que se hubiera quedado abierto (`removeGroup`). Si no,
   la creación falla con `BUSY`.
3. `WifiP2pManager.createGroup(channel, config)` con un `WifiP2pConfig` que lleva ese nombre y
   contraseña, banda `AUTO` y modo no persistente (el sistema no guarda el grupo para la
   próxima vez). Esta variante con credenciales propias existe desde Android 10 (API 29), que es
   el `minSdk` de la app.
4. El móvil pasa a ser el **dueño del grupo** (*Group Owner*, GO): funciona como un punto de
   acceso Wi‑Fi con cifrado **WPA2‑Personal**, tiene la IP **192.168.49.1** y reparte por DHCP
   direcciones 192.168.49.x a quien se una.
5. El grupo tarda unos milisegundos en existir. Se pregunta con `requestGroupInfo` cada 250 ms,
   hasta 10 s. Antes la app esperaba 500 ms a ciegas.
6. Si algún fabricante no respeta el nombre o la contraseña pedidos, se usan los que el sistema
   ha puesto de verdad (`CreatedGroup.credentials`), que son los que se publicarán por BLE.

En el Realme de pruebas (Android 16) el grupo salió en **5 GHz, canal 36 (5180 MHz)**, con la
interfaz de red `p2p0`.

### Paso 2 · El receptor abre el puerto TCP

`WifiDirectTransport.openServer(8988)`: un `ServerSocket` escuchando en todas las interfaces
(`0.0.0.0:8988`), con `SO_REUSEADDR` para poder reabrirlo enseguida tras una sesión anterior.

Se abre **antes** de anunciarse: así es imposible que el emisor llegue y no haya nadie
escuchando.

### Paso 3 · El receptor se anuncia por Bluetooth LE

`BleTransport.advertise` hace dos cosas a la vez, y las dos se paran al cancelar:

**a) Servidor GATT con la baliza**

- Servicio `e4a2c8aa-62f1-4b9c-8d39-57c8d908b33f`.
- Una característica de solo lectura, `e4a2c8ab-62f1-4b9c-8d39-57c8d908b33f`, cuyo valor es la
  **baliza** (`ReceiverBeacon`) en JSON, de unos 125 bytes:

  ```json
  {"protocolVersion":1,"displayName":"Prueba","networkName":"DIRECT-UY-CincoPelao",
   "passphrase":"<16 caracteres>","port":8988}
  ```

**b) Anuncio BLE** (*advertising* "legacy", conectable)

Un anuncio BLE clásico tiene **31 bytes** útiles, más otros 31 en la *respuesta al escaneo*, que
el escáner pide activamente. Se reparten así:

```
Paquete de anuncio (ADV_IND)                     Respuesta al escaneo (SCAN_RSP)
┌──────────────────────────┐                     ┌──────────────────────────────────────────┐
│ Flags            3 bytes │                     │ len │ 0x21 │ UUID (16 B) │ v │ nombre   │
│ UUIDs 128 bits  18 bytes │ ← UUID del servicio │  1  │  1   │     16      │ 1 │ ≤ 12 B   │
└──────────────────────────┘   (21 de 31 bytes)  └──────────────────────────────────────────┘
                                                   AD 0x21 = "Service Data, UUID de 128 bits"
                                                   2 + 16 + 1 + 12 = 31 bytes justos
```

Por eso el nombre visible está limitado a **12 bytes en UTF‑8**: la pantalla muestra el contador
y `BleAdvert.truncateUtf8` recorta sin partir una "ñ" ni un emoji.

- El nombre del móvil **no** se incluye (`setIncludeDeviceName(false)`): solo viaja el nombre que
  elige el usuario.
- Modo `LOW_LATENCY`: un anuncio cada ~100 ms. En el Realme el sistema registró un intervalo de
  160 × 0,625 ms = 100 ms y una potencia de +1 dBm.

### Paso 4 · El emisor escanea

`BleTransport.scan`, desde `TransmitViewModel.startScan`:

- `ScanFilter` por el UUID del servicio: el propio chip Bluetooth descarta todo lo demás
  (auriculares, relojes…).
- `SCAN_MODE_LOW_LATENCY` y `CALLBACK_TYPE_ALL_MATCHES`: se recibe cada anuncio, no solo el
  primero, y así se puede seguir la intensidad de la señal.
- De cada resultado se lee el *service data* (versión y nombre). Los resultados sin respuesta al
  escaneo se ignoran hasta que llegue uno completo.

La lista de receptores (`NearbyReceiver`, reglas puras con tests):

- **RSSI suavizado**: `nuevo = (2·anterior + medida) / 3`, para que no baile.
- **Distancia estimada** con el modelo de pérdida en espacio libre:
  `d = 10^((TxPower − RSSI) / (10·n))`, con TxPower = −59 dBm a 1 m y n = 2. Paredes y cuerpos
  la cambian mucho; es orientativa.
- Un receptor desaparece si pasan **3 s** sin anuncios (anuncia unas 10 veces por segundo).
- Al volver a hacerse visible, un receptor estrena dirección BLE aleatoria: si llega uno con el
  mismo nombre y otra dirección, sustituye al antiguo en vez de salir duplicado.
- Si anuncia otra versión del protocolo, aparece pero no se puede pulsar.
- La dirección que se ve (`CE:59:…`) es una **dirección aleatoria** de BLE (*resolvable private
  address*): Android la cambia cada cierto tiempo y no identifica al móvil.

Antes, el emisor abría una conexión GATT con cada dispositivo que veía, solo para saber su
nombre. Ahora el nombre llega en el anuncio y solo se conecta al elegido.

### Paso 5 · El emisor lee la baliza GATT

Al pulsar un receptor se **para el escaneo** y `BleTransport.readBeacon(address)` hace:

1. `connectGatt(autoConnect = false, TRANSPORT_LE)`: conexión directa por LE.
2. `requestMtu(517)`. La MTU de ATT por defecto es de 23 bytes (20 útiles). Con 517 caben
   512 bytes de valor en una sola lectura, que es el máximo de un atributo y el límite que
   `ReceiverBeacon.decode` acepta.
3. `discoverServices()` y `readCharacteristic(baliza)`.
4. Si la MTU negociada fuera pequeña, Android lo resuelve con **lecturas largas** (*Read Blob*):
   pide el valor por trozos con un `offset`, y el servidor responde con `valor[offset..]`. El
   registro del receptor muestra cada trozo ("lee la baliza (offset N, M B)").
5. La baliza se **valida** antes de usarla: como mucho 512 bytes, JSON correcto, versión 1,
   nombre de red `DIRECT-xy…` de 32 bytes como mucho, contraseña de 8 a 63 caracteres ASCII
   imprimibles y puerto entre 1 y 65535.
6. Se cierra el GATT pase lo que pase (`disconnect` + `close` en un `finally`). Tiempo límite: 15 s.

El panel técnico muestra la MTU negociada y lo que tardó la lectura.

### Paso 6 · El emisor se une al grupo Wi‑Fi Direct

`WifiDirectTransport.joinGroup(beacon)`:

1. Sale de cualquier grupo anterior (`cancelConnect` + `removeGroup`).
2. Se registra para el aviso del sistema `WIFI_P2P_CONNECTION_CHANGED_ACTION` **antes** de
   conectar, para no perdérselo.
3. `WifiP2pManager.connect(channel, config)` con un `WifiP2pConfig` que lleva **nombre de red y
   contraseña** (Android 10+). Con esta forma Android se une directamente a un grupo que ya
   existe, como cliente:
   - **No hay búsqueda de *peers*** ni negociación de quién es el dueño (*GO negotiation*).
   - **No hay WPS** (ni botón ni PIN), así que **nadie tiene que aceptar una invitación**.
   - Por debajo (`wpa_supplicant`), busca el SSID del grupo, se asocia y hace el *4‑way
     handshake* de WPA2 con una clave derivada de la contraseña. Esa clave es
     PBKDF2‑HMAC‑SHA1(contraseña, SSID, 4096 iteraciones, 256 bits). Después pide IP por DHCP al
     dueño y recibe una 192.168.49.x.
4. Espera un aviso en el que `groupFormed = true`, `isGroupOwner = false`, haya dirección del
   dueño y el nombre del grupo coincida, o no venga, porque algunas versiones no lo adjuntan. El
   aviso es "pegajoso" (al registrarse llega el último estado), así que se ignoran estados
   antiguos y otros grupos.
5. Tiempo límite: 30 s. Si salta, se cancela la conexión pendiente.

Antes, el emisor buscaba *peers* y escogía el primero cuyo nombre **empezara** por el que había
leído por BLE. Luego lanzaba una conexión normal, que en muchos móviles hace salir un diálogo de
aceptación en el otro. La contraseña leída por BLE no se usaba para nada.

### Paso 7 · Conexión TCP

- **Emisor**: `Socket.connect(192.168.49.1:8988)` con 5 s de límite y hasta **5 intentos**
  separados 1 s, porque justo tras unirse la ruta o el DHCP pueden tardar un instante.
- **Receptor**: `accept()` devuelve la conexión, el receptor **deja de anunciarse por BLE** y
  **cierra el `ServerSocket`**. Solo se atiende a un emisor por sesión.
- Los dos ponen `TCP_NODELAY`, que desactiva el algoritmo de Nagle: los mensajes cortos, como
  HELLO o ACK, salen al momento en vez de esperar a juntarse con otros.

A partir de aquí todo viaja por este socket. TCP garantiza orden y que no se pierden bytes; el
protocolo de Cinco Pelao pone encima los límites, la versión y las comprobaciones.

### Paso 8 · Saludo (HELLO) y código de emparejamiento

Siempre empieza el emisor:

```
emisor   → HELLO {protocolVersion, deviceName, model, androidVersion, appVersion, nonce}
receptor → HELLO {…, nonce}
```

- `nonce`: 16 bytes de `SecureRandom` en hexadecimal, nuevos en cada sesión.
- **Código de emparejamiento** (`verificationCode`): los dos calculan
  `SHA-256("menor:mayor")`, con los dos nonces ordenados para que dé igual quién es quién. De
  ahí toman los 4 primeros bytes como entero sin signo y se quedan con `mod 10000`, que da 4
  cifras. Si las dos pantallas enseñan el mismo código, los dos móviles están hablando entre sí.
- Si el primer mensaje no es un HELLO, la sesión se corta (`Unexpected`). Si es un ERROR, se
  muestra su motivo.

### Paso 9 · Petición (REQUEST) y aprobación

1. El receptor elige en pantalla qué quiere y pulsa "Pedir":
   `REQUEST {selection: {subjectsAndEvents, subjectTasks, generalTasks, schedule}}`.
2. La selección se **normaliza en los dos lados** (`ShareSelection.normalized`): si se piden
   tareas de asignatura sin asignaturas, se quitan. El receptor no deja marcarlo, y el emisor lo
   corrige aunque le llegue igualmente, por si el otro móvil tuviera una versión distinta.
3. El emisor prepara el paquete con los datos de su **instancia activa**
   (`buildSharePayload`, función pura con tests):
   - Las asignaturas y eventos van **sin nota** (`score = null`) y sin marcar como hechos.
   - Las tareas van como **pendientes**. Las archivadas no se envían.
   - Cada tarea se clasifica, como de asignatura o general, **según su tarea raíz**: una familia
     de subtareas viaja siempre entera.
   - El horario se exporta a **PNG** (`ShareScheduleStore.exportCurrent`). Si es un PDF, se pinta
     a 3× con todas sus páginas, una debajo de otra.
4. El emisor ve qué le piden con los recuentos ("Asignaturas y eventos 3 · 12", "Horario PNG
   340 KB"…) y elige:
   - **Enviar** → paso 10.
   - **Rechazar** → `ERROR {reason: "El otro móvil ha rechazado el envío"}`. El receptor lo
     recibe como `Rejected` y muestra el motivo.

### Paso 10 · Datos (PAYLOAD) y confirmación (ACK)

1. El emisor manda `PAYLOAD`, cuyo cuerpo es binario (ver [sección 5](#5-mensajes)): JSON con
   los datos más el PNG del horario. Se escribe en **bloques de 16 KB** y cada bloque actualiza
   el progreso (`Transferring(0…1)`), que el icono pinta como un anillo.
2. El receptor lee la trama con la misma granularidad: el progreso avanza a la vez en los dos.
3. Al terminar, el receptor:
   - comprueba el **CRC32** de la trama, que va en la propia trama;
   - calcula el **SHA‑256** del cuerpo recibido;
   - contesta `ACK {sha256}`.
4. El emisor compara ese SHA‑256 con el del cuerpo que envió. Si no coinciden, la sesión falla.
   Si coinciden, cierra su socket.
5. **Cierre ordenado**: el receptor no deshace nada hasta leer el final del flujo (el FIN de TCP)
   que manda el emisor al cerrar, como mucho 3 s (`awaitPeerClose`). Si deshiciera el grupo nada
   más mandar el ACK, la interfaz `p2p0` podría desaparecer con el ACK aún en camino, y el emisor
   vería "Software caused connection abort" tras una transferencia correcta. Este bug lo encontró
   el autor con dos móviles. Lo mismo hace el emisor tras mandar un ERROR al rechazar.
6. Los dos guardan cifras para el panel técnico: bytes en el cable, tiempo, velocidad (B/s y
   Mbit/s), CRC32 y SHA‑256 abreviado. El cronómetro del receptor arranca con el **primer
   bloque**, no al enviar la petición, para no contar el rato que el emisor tarda en aceptar.

¿Por qué CRC32 y SHA‑256 si TCP ya comprueba los datos? La suma de TCP es de 16 bits y bastante
débil, y además se recalcula en cada salto. El CRC32 de cada trama detecta daños por el camino
o fallos de memoria. El SHA‑256 en el ACK demuestra **de extremo a extremo** que el receptor
tiene exactamente lo que el emisor envió.

### Paso 11 · Cierre

Quien manda el último mensaje (ACK o ERROR) ya ha esperado a que el otro cierre (paso 10).
Después, pase lo que pase (éxito, error o cancelación), el bloque `finally` de cada sesión:

- cierra el socket TCP (y el `ServerSocket` en el receptor);
- deshace el grupo Wi‑Fi Direct (`removeGroupQuietly`) dentro de `NonCancellable`: aunque la
  pantalla se cierre, la limpieza llega a ejecutarse. En el receptor esto destruye el grupo; en
  el emisor, sale de él.

El GATT ya se cerró en el paso 5, y el anuncio y el escaneo BLE en los pasos 7 y 5.

### Paso 12 · Importación en el receptor

1. La fase pasa a `Received`: el receptor ve el resumen y pone un nombre, que por defecto es el
   del otro móvil.
2. `InstanceRepository.importAsNewInstance`:
   - crea una **base de datos nueva**, una por instancia;
   - limpia referencias rotas (`withoutDanglingReferences`): un evento sin su asignatura se
     descarta, y una tarea sin su asignatura o su madre pasa a ser general o independiente;
   - inserta las tareas con las madres primero (`parentsFirst`), todo en **una transacción**;
   - activa la instancia nueva.
3. Si llegó horario, `ShareScheduleStore.import` comprueba que el PNG se puede decodificar
   (solo las dimensiones, sin cargarlo entero) **antes** de escribirlo en
   `files/SCHEDULE_<id>/<id>.png`, y guarda su estado en DataStore.
4. Si algo falla, se muestra el error y se puede reintentar sin volver a recibir: los datos
   siguen en memoria. Cada sesión tiene los suyos y nada se acumula entre sesiones.

---

## 4. Formato de las tramas

Todo lo que viaja por el socket son **tramas**. Los enteros van en *big‑endian*, el orden de
red, que es el que usa `DataOutputStream`.

```
 0      4        5      6          10               10+N     14+N
 ┌──────┬────────┬──────┬──────────┬────────────────┬────────┐
 │ MAGIC│ VERSIÓN│ TIPO │ LONGITUD │ CUERPO         │ CRC32  │
 │ 4 B  │ 1 B    │ 1 B  │ 4 B (N)  │ N bytes        │ 4 B    │
 └──────┴────────┴──────┴──────────┴────────────────┴────────┘
   "5PLO"   = 1    1..5   ≤ límite   JSON o binario   del cuerpo
 0x35504C4F
```

- **MAGIC** `0x35504C4F` ("5PLO" en ASCII): si alguien que no es Cinco Pelao se conecta al
  puerto, se nota en los 4 primeros bytes.
- **VERSIÓN**: si no es la nuestra, error `IncompatibleVersion` indicando la del otro.
- **TIPO**: 1 HELLO · 2 REQUEST · 3 PAYLOAD · 4 ACK · 5 ERROR.
- **LONGITUD**: se compara con el límite **antes de reservar memoria**:
  - 64 KB para los mensajes de control (HELLO, REQUEST, ACK y ERROR);
  - 32 MB para PAYLOAD.

  Antes, la app reservaba lo que dijera la cabecera: un número corrupto podía pedir 2 GB.
- **CRC32** (el estándar IEEE, `java.util.zip.CRC32`) del cuerpo.
- Coste fijo: **14 bytes** por trama.

El orden de las comprobaciones al leer es magic → versión → tipo → longitud → cuerpo → CRC32
→ JSON. Si la conexión se corta a mitad, el error es `Truncated`.

### Ejemplo real

`ERROR {"reason":"no"}` en el cable (29 bytes):

```
35 50 4C 4F                                       "5PLO"
01                                                versión 1
05                                                tipo 5 = ERROR
00 00 00 0F                                       longitud 15
7B 22 72 65 61 73 6F 6E 22 3A 22 6E 6F 22 7D      {"reason":"no"}
D4 01 AD 1E                                       CRC32 del cuerpo
```

## 5. Mensajes

Los cuerpos de control son JSON en UTF‑8 (`kotlinx.serialization`, con `ignoreUnknownKeys`:
dentro de la misma versión se pueden añadir campos sin romper la compatibilidad).

| Tipo | Quién | Cuerpo |
|---|---|---|
| HELLO | los dos | `{"protocolVersion":1,"deviceName":"…","model":"Realme RMX5054","androidVersion":"16 (API 36)","appVersion":"2.0","nonce":"<32 hex>"}` |
| REQUEST | receptor | `{"selection":{"subjectsAndEvents":true,"subjectTasks":true,"generalTasks":false,"schedule":true}}` |
| PAYLOAD | emisor | binario, ver abajo |
| ACK | receptor | `{"sha256":"<64 hex>"}` |
| ERROR | cualquiera | `{"reason":"texto para el usuario"}` |

**Cuerpo de PAYLOAD:**

```
┌───────────────────┬──────────────────────────────┬─────────────────────────┐
│ longitud JSON 4 B │ JSON de DataToSend           │ PNG del horario (resto) │
└───────────────────┴──────────────────────────────┴─────────────────────────┘
```

- `DataToSend` es `{"subjects":[…],"events":[…],"tasks":[…]}` con las entidades de Room tal cual
  (ids incluidos: la importación los conserva en la base de datos nueva).
- Si no hay horario, el cuerpo acaba tras el JSON.
- Se valida que la longitud del JSON quepa en el cuerpo (`Malformed` si no).

## 6. Cancelación, errores y tiempos límite

**Modelo de vida.** Toda la sesión es una sola corrutina (`sessionJob`) del ViewModel. Pulsar
Cancelar, salir de la pantalla o "Enviar a otro" la cancela, y la cancelación se propaga así:

- **Flows de BLE** (`advertise`, `scan`): su `awaitClose` para el anuncio o el escaneo y cierra
  el servidor GATT.
- **Lecturas, escrituras, `accept()` y `connect()` de sockets**: no se pueden interrumpir desde
  Java. `cancellableBlocking` lanza un "vigilante" que, si la corrutina se cancela, **cierra el
  socket**, y eso desbloquea la operación. El error de E/S que provoca ese cierre se convierte
  en cancelación: no se muestra como "conexión perdida". Fue un bug encontrado en el móvil y
  tiene test.
- **`finally`**: socket cerrado y grupo deshecho (paso 11).

**Tiempos límite**

| Operación | Límite |
|---|---|
| Lectura de la baliza GATT | 15 s |
| Creación del grupo (esperar a que exista) | 10 s (40 × 250 ms) |
| Unirse al grupo | 30 s |
| Conexión TCP | 5 s × 5 intentos |
| Desaparecer de la lista de receptores | 3 s sin anuncios |
| Esperar el cierre del otro tras el último mensaje | 3 s |
| Esperas a una decisión del usuario (elegir, aceptar) | sin límite (se sale con Cancelar) |

Limitación conocida: **no hay tiempo límite de lectura en el socket** mientras se espera al
otro. Si el otro móvil desaparece sin cerrar la conexión (por ejemplo, se queda sin batería), la
pantalla puede quedarse esperando hasta pulsar Cancelar. Se puede mejorar con un *keepalive*
propio si en las pruebas resulta molesto.

**Mensajes de error** (`describeShareError`): los errores propios (protocolo, BLE, Wi‑Fi Direct)
traen su texto en español. Un tiempo agotado → "El otro móvil ha tardado demasiado…". Falta de
permisos → "Falta algún permiso…". Otro error de E/S → "Se ha perdido la conexión…". El detalle
técnico (clase y mensaje) queda en la consola con la etiqueta `ERR`.

## 7. Seguridad: qué protege y qué no

**Protegido**

- **El enlace Wi‑Fi Direct va cifrado con WPA2** (AES‑CCMP) y una contraseña aleatoria de
  ~93 bits que **cambia en cada sesión**: quien no la tenga no puede escuchar ni unirse. Los
  datos van en claro dentro del TCP, pero dentro de ese túnel cifrado.
- **Solo una conexión por sesión**: tras el primer `accept()` se cierra el puerto y se deja de
  anunciar.
- **Nada sale sin permiso**: el emisor ve qué le piden y lo acepta o rechaza.
- **Nada entra sin permiso**: el receptor ve el resumen y decide si lo guarda (o lo descarta).
- **Entradas validadas**: magic, versión, tipo, límites de tamaño antes de reservar memoria, CRC32,
  JSON, baliza y que el horario sea una imagen. Un emisor malicioso no puede tumbar la app con
  un tamaño gigante ni colar referencias rotas en la base de datos.
- **Privacidad**: el anuncio BLE no lleva el nombre del móvil; las direcciones BLE son
  aleatorias; la MAC de Wi‑Fi Direct que ve el emisor (la del dueño del grupo) también es
  aleatoria desde Android 10 y ya viaja al aire en cada trama del grupo, así que enseñarla no
  revela nada nuevo (la propia la oculta Android a las apps); no se comparten notas ni progreso; **la contraseña no aparece** en pantalla, en la
  consola ni en el log de Android (antes salía en un Toast y en el log).

**No protegido (decisiones conscientes)**

- **La baliza BLE se lee sin cifrar.** Mientras Recibir está visible, cualquiera a unos metros
  con un escáner BLE puede leer el nombre y la contraseña de la red. Con ellos podría unirse al
  grupo y adelantarse al emisor de verdad. En ese caso:
  - al emisor de verdad no le llegaría la conexión TCP (el puerto ya está cerrado) y vería un
    error;
  - el receptor vería en pantalla el nombre del intruso, y un código que no coincide con el del
    emisor;
  - el intruso solo podría **ofrecer** datos, que el receptor tendría que aceptar y guardar
    expresamente. No puede leer nada del receptor.

  Cifrar la lectura GATT obligaría a emparejar los móviles por Bluetooth, con el diálogo del
  sistema. Se descartó por comodidad.
- **El código de emparejamiento detecta conexiones equivocadas, no un atacante sofisticado.** Los
  nonces viajan en claro (dentro de WPA2), así que alguien que hiciera de intermediario entre los
  dos podría reenviarlos y obtener el mismo código. Para eso tendría que estar dentro del grupo
  WPA2 de cada uno a la vez. Una protección criptográfica real (intercambio de claves
  autenticado) sería excesiva para lo que se comparte.

## 8. Permisos por versión de Android

`SharePermissions.required` (con tests). `minSdk` es 29 (Android 10).

| Android | Transmitir | Recibir |
|---|---|---|
| 10–11 (29–30) | `ACCESS_FINE_LOCATION` | `ACCESS_FINE_LOCATION` |
| 12–12L (31–32) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` | `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` |
| 13+ (33+) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES` | `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `NEARBY_WIFI_DEVICES` |

- Hasta Android 12L, Wi‑Fi Direct necesita además la **ubicación del sistema** activada.
- `BLUETOOTH_SCAN` y `NEARBY_WIFI_DEVICES` se declaran con `neverForLocation`: la app promete no
  sacar la ubicación de los escaneos, y a cambio Android no le exige el permiso de ubicación a
  partir de la 13.
- En el manifiesto, `BLUETOOTH` y `BLUETOOTH_ADMIN` solo aplican hasta Android 11, y
  `ACCESS_FINE_LOCATION` hasta Android 12L.
- `INTERNET` (sockets), `ACCESS_WIFI_STATE` y `CHANGE_WIFI_STATE` son permisos normales: se
  conceden al instalar.
- BLE y Wi‑Fi Direct se declaran como `uses-feature required="false"`: la app se puede instalar
  en móviles sin ellos; solo la compartición avisará de que no se puede.

## 9. Qué enseña el panel de detalles técnicos

| Sección | Datos | Paso |
|---|---|---|
| Este móvil | Modelo, Android y API, versión de la app y del protocolo, MAC de Wi‑Fi Direct (normalmente "oculta por Android") | 0 |
| Bluetooth LE | Tamaño de la baliza, modo y potencia del anuncio · receptor elegido, su MAC BLE, RSSI y distancia, MTU y tiempo de lectura GATT | 3–5 |
| Wi‑Fi Direct | SSID, banda, canal y frecuencia, interfaz (`p2p0`…), MAC del dueño, IP propia y del dueño | 1, 6 |
| Socket TCP | Puerto en escucha, extremos local y remoto, opciones | 2, 7 |
| Protocolo | Modelo y versión del otro, código, bytes, tiempo, velocidad, CRC32 y SHA‑256 | 8–10 |

La **consola** registra cada paso con hora al milisegundo y una etiqueta: `BLE`, `GATT`, `P2P`,
`TCP`, `SHA`, `APP`, `DB` o `ERR`. Cada trama aparece como
`→ PAYLOAD 360462 B crc=1A2B3C4D` (→ enviada, ← recibida). El botón de copiar la lleva al
portapapeles para adjuntarla a un informe de fallo.

## 10. Mapa del código

```
sharing/
├── protocol/                 Kotlin puro, con tests
│   ├── ShareProtocol.kt      versión, magic, puerto, límites, excepciones
│   ├── FrameCodec.kt         tramas: escribir/leer/validar, CRC32, SHA-256
│   ├── ShareMessage.kt       HELLO, REQUEST, PAYLOAD, ACK, ERROR
│   ├── ShareConnection.kt    mensajes sobre un socket + cancellableBlocking
│   ├── ShareSelection.kt     qué pide el receptor
│   ├── SharePayload.kt       buildSharePayload, normalized, resumen
│   ├── ReceiverBeacon.kt     baliza GATT + GroupCredentials
│   └── BleAdvert.kt          UUIDs y formato del anuncio (nombre ≤ 12 B)
├── session/                  Kotlin puro, con tests
│   ├── ShareSession.kt       SenderSession, ReceiverSession, código, informe
│   ├── SharePhase.kt         fases de las pantallas
│   └── SharePermissions.kt   permisos por versión y papel
├── ble/BleTransport.kt       anuncio + GATT servidor, escaneo, lectura de la baliza
├── wifidirect/WifiDirectTransport.kt   grupo, unirse, sockets
├── ShareEnvironment.kt       permisos, radios, HELLO de este móvil
└── ShareScheduleStore.kt     horario ↔ PNG

ui/sharing/
├── ShareActivities.kt        TransmitActivity, ReceiveActivity, ajustes del sistema
├── TransmitScreen.kt / TransmitViewModel.kt
├── ReceiveScreen.kt  / ReceiveViewModel.kt
├── ShareStatusIcon.kt        iconos animados por fase
├── ShareComponents.kt        cabecera, tarjeta de ajustes, código, panel técnico, consola
├── ShareModels.kt            receptores cercanos, consola, filas técnicas (puro)
├── ShareFormat.kt            bytes, velocidad, canal Wi‑Fi, RSSI, distancia (puro)
└── ShareErrors.kt            textos de error
```

Tests: `app/src/test/.../sharing/` (tramas, baliza, anuncio, selección, sesiones completas por
sockets, cancelación, permisos) y `.../ui/sharing/ShareUiRulesTest.kt`.
