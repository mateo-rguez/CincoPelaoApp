# Refactor de CincoPelao

Seguimiento del refactor de la app: plan por fases, lo hecho y lo que queda.
Rama de trabajo: `rework`. Última actualización: 8 de noviembre de 2026.

## Punto de partida

Análisis inicial (antes de empezar): 44 archivos Kotlin en un solo paquete, archivos de hasta
1.300 líneas, lógica de negocio dentro de los composables, un `DatabaseManager` global con estado
de Compose, recordatorios gestionados desde la interfaz, base de datos sin migraciones y varios
bugs (crashes, carreras entre corrutinas, datos que podían perderse).

Para no tocar la versión instalada, la app nueva usa otro `applicationId`
(`io.github.mateorguez.cincopelao`); la antigua (`com.example.cincopelao`) sigue intacta.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Red de seguridad: esquema de Room exportado, primeros tests, `.gitignore`, `gradlew` | ✅ |
| 1 | Dependencias en el version catalog, fuera las que sobraban, código muerto, APIs deprecadas | ✅ |
| 2 | Paquetes por capa y renombrado a `io.github.mateorguez.cincopelao` | ✅ |
| 3 | Capa de datos: `InstanceRepository`, repositorios, operaciones atómicas | ✅ |
| 4 | Recordatorios: planificador puro + reconciliación con WorkManager | ✅ |
| 5 | Interfaz: tema, navegación, un ViewModel por pantalla, rendimiento | ✅ |
| 6 | Compartición BLE / Wi‑Fi Direct | 🧪 código hecho; falta probar con dos móviles |
| 7 | Mejoras y funcionalidades nuevas | ⏳ |

**Siguiente paso:** probar la fase 6 con **dos móviles Android** siguiendo la lista de
[Pruebas con dos móviles](#pruebas-con-dos-móviles-fase-6) y arreglar lo que salga. Mientras
tanto se puede empezar la fase 7.

## Hecho

### Fase 0 — Red de seguridad
- `24f9f96` `.gitignore` limpio y `gradlew` ejecutable.
- `d57564f` Esquema de Room (v8) exportado a `app/schemas/`.
- `c4fb4f7`, `eebf472` Primeros tests (estadísticas y hora de los recordatorios).

### Fase 1 — Build y dependencias
- `2620a53` Todo en `libs.versions.toml`; fuera Google Places, PermissionX y AppCompat
  (solo había imports sin usar; 49 librerías transitivas menos).
- `573d958` Código muerto y bloques comentados fuera.
- `836cb7a` APIs deprecadas sustituidas y Material 2 eliminado (88 → 13 avisos; los que quedan
  están en BLE / Wi‑Fi Direct).

### Fase 2 — Estructura
- `b462961`, `c05019a` Paquetes `data/`, `domain/`, `notifications/`, `sharing/`, `ui/<pantalla>`, `di/`.

### Fase 3 — Datos
- `6a4c89b` `InstanceRepository` inyectado en lugar del `DatabaseManager` global.
- `3a80d97` Repositorios separados, mantenimiento de tareas único a nivel de app, borrar/deshacer en transacciones.
- `bd82953` Estadísticas sin crash con peso nulo ni `NaN`.

### Fase 4 — Recordatorios
- `ab93ea0` `ReminderPlanner` + `ReminderManager`; permiso de notificaciones; ids que no se pisan;
  hora local correcta.
- `f6bdd1e` Recordatorios de todas las instancias (decisión del autor), no solo de la activa.

### Fase 5 — Interfaz
- `ad4bca0` Tema centralizado (`ThemeRepository`, `LocalDarkTheme`, fuentes y colores con nombre).
- `70212e4` Navegación sin destinos duplicados; claves en las listas.
- `41c819f` Tareas · `f289420` Completadas.
- `54abb6a` Lista de asignaturas · `318d1b4` Detalle y estadísticas (`GradeCalculator`) ·
  `1e60fa3` Alta y edición.
- `78e57c0` Calendario · `bbe7a7a` Horario · `77c8465` Notificaciones · `3f74327` Instancias · `1f25337` ids unificados a `Long` · `94c89e4` formato de números.

### Fase 6 — Compartición
- `b3df804` Protocolo: tramas `"5PLO" | versión | tipo | longitud | cuerpo | CRC32`, límite de
  tamaño por tipo, mensajes HELLO/REQUEST/PAYLOAD/ACK/ERROR y baliza BLE con las credenciales
  del grupo.
- `4e90181` Selección: tareas de asignatura (solo con asignaturas) y tareas generales por separado;
  no se comparten notas ni progreso.
- `d352500` Sesiones emisor/receptor probadas por sockets locales, código de emparejamiento,
  fases y permisos por versión.
- `4871c8b` `BleTransport` y `WifiDirectTransport`: todo se para al cancelar; el emisor se une al
  grupo con nombre y contraseña (sin buscar peers ni aceptar invitaciones).
- `fcf38a9` Pantallas nuevas con panel de detalles técnicos y consola; fuera `MainViewModel`.
- `26c5b4e` Iconos animados por fase (Compose, sin Lottie) y ajustes tras probar en el Realme.
- `68eab36` Tolerancia a fabricantes que cambian el grupo Wi‑Fi Direct.

Cómo funciona ahora (explicación detallada, capa a capa: [`COMPARTICION.md`](COMPARTICION.md)):

```
Recibir                                   Transmitir
 crea grupo DIRECT-xy-CincoPelao (GO)
 abre ServerSocket :8988
 anuncia BLE (UUID + nombre visible) ───►  escanea BLE, lista receptores (RSSI, distancia)
 GATT: baliza {red, contraseña, puerto} ◄─  lee la baliza (MTU 517) y cierra el GATT
                                     ◄───  se une al grupo con red + contraseña (Android 10+)
 accept() ◄──────────── TCP ─────────────  connect 192.168.49.1:8988
 HELLO ↔ HELLO (código de 4 cifras en las dos pantallas)
 REQUEST ───────────────────────────────►  el usuario acepta o rechaza
 PAYLOAD ◄───────────────────────────────  JSON + PNG del horario (con progreso)
 ACK(sha256) ───────────────────────────►  comprueba la suma
 nombre de instancia → importa
```

Probado en un solo móvil (Realme, Android 16): grupo en 5 GHz, socket, anuncio BLE, cancelar
deshace el grupo y para el anuncio, el escaneo se para al salir. Sin probar: todo lo que
necesita el otro extremo.

### Bugs corregidos más relevantes
- Crashes: editar una instancia ya borrada; `instanceId!!` en Horario; `subject!!` en el
  Calendario; estadísticas con un evento sin peso; franja horaria vacía en los recordatorios.
- Datos: deshacer borrados y "eliminar solo esta" podían perder subtareas o cerrar la app;
  importar tareas sin sus asignaturas fallaba en silencio; el guardado de una asignatura podía
  cancelarse; editar un evento nuevo modificaba otro.
- Recordatorios: nunca se pedía el permiso (no llegaba ninguno); eventos y tareas se pisaban;
  medianoche en UTC; avisos fantasma al borrar o completar.
- Compartición: una segunda recepción importaba los datos de la primera; tareas de asignatura
  enviadas sin su asignatura; escaneo BLE que no se paraba nunca y anuncio que seguía tras
  `stopServer`; la contraseña del grupo en un Toast; tamaños sin validar; fallos de importación
  silenciosos; grupo y socket vivos al salir de la pantalla.
- Interfaz: parpadeo del tema claro en modo oscuro; doble navegación en la barra inferior;
  formularios que perdían lo escrito al girar o al cambiar datos; selectores de fecha cuyo
  Cancelar no deshacía; "Completada el java.text.SimpleDateFormat@…"; carga de imágenes y PDF
  en el hilo principal.

Tests: 120 (JUnit + Robolectric con Room y WorkManager de pruebas, y sockets TCP locales para la
compartición) — `./gradlew testDebugUnitTest`.

## Pendiente

### Pruebas con dos móviles (fase 6)

Abrir **Detalles técnicos** en los dos móviles: la consola cuenta qué paso falla. Si algo va
mal, copiar el registro (botón de copiar) y pasárselo a Claude.

Preparación: instalar la misma versión en los dos (`./gradlew installDebug` con cada uno por
USB), y en el emisor tener una instancia de prueba con asignaturas, eventos con nota, tareas de
asignatura con subtareas, tareas generales, alguna tarea archivada y un horario.

1. **Permisos**: en un móvil sin permisos concedidos, abrir Transmitir y Recibir → se piden solo
   los necesarios; si se deniegan, aparece la tarjeta "Antes de empezar" con los botones.
2. **Radios apagadas**: con Bluetooth o Wi‑Fi apagados → la tarjeta lo indica y "Activar" abre
   el ajuste; al volver, la pantalla sigue sola.
3. **Descubrimiento**: Recibir → nombre → "Hacerme visible". En Transmitir aparece en ~1 s con
   RSSI y distancia; al alejarse, el RSSI baja; al cancelar en Recibir, desaparece en ~5 s.
4. **Conexión**: pulsar el receptor → fases "Contactando", "Conectando por Wi‑Fi Direct",
   "Presentándose" → el **mismo código de 4 cifras** en los dos. Ningún móvil debe pedir aceptar
   una invitación Wi‑Fi Direct.
5. **Envío completo**: pedir todo → en el emisor salen los recuentos → Enviar → progreso en los
   dos → el receptor guarda con un nombre → la instancia nueva está activa con todo:
   - eventos **sin nota** y tareas **pendientes**; tareas archivadas no llegan;
   - subtareas bajo su madre; horario visible en Horarios.
6. **Combinaciones**: solo tareas generales (no llega ninguna asignatura); asignaturas sin
   tareas; "Tareas de asignaturas" debe estar desactivada si no se marcan asignaturas; solo
   horario; pedir horario a un emisor que no tiene (llega sin él, sin error).
7. **Rechazo**: el emisor pulsa Rechazar → el receptor ve "El otro móvil ha rechazado el envío".
8. **Dos envíos seguidos** (el bug antiguo): recibir A, luego "Enviar a otro" y recibir B con
   otra selección → la segunda instancia tiene **los datos de B**, no los de A.
9. **Cortes**: cancelar o salir de la pantalla a mitad en cada fase, en los dos lados → el otro
   muestra un error claro y se puede reintentar; después, el Wi‑Fi normal sigue funcionando.
10. **Horario grande** (PDF de varias páginas) → progreso fluido y sin cierres.
11. **Alcance**: alejarse con los móviles hasta que falle → error claro, sin cuelgues.
12. **Otras marcas**: si es posible, probar con un móvil de otro fabricante y con Android 10–12
    (allí se pide ubicación y hay que tener la ubicación activada).

### Fase 7 — Mejoras (propuestas)
- Widgets de próximos exámenes y tareas.
- Copia de seguridad: exportar e importar a archivo.
- Horario estructurado (no solo imagen) que pueda avisar de las clases.
- Media ponderada global por curso.
- Material You con colores dinámicos.
- Publicación: R8/minify activado y firma de release.
- Animaciones Lottie para los iconos de compartir (si el autor encuentra unas que le gusten:
  basta con cambiar `ShareStatusIcon`).
- Contraste del azul del tema en modo oscuro en el resto de pantallas (en compartir ya se usa
  `ShareAccentDark`).

### Aplazado a propósito
- Actualizar versiones de librerías (Compose, Kotlin, AGP…): el autor prefirió no hacerlo aún.
- Textos de la interfaz a `strings.xml` (~340 textos): la app seguirá solo en español por ahora;
  se hará si se añade otro idioma.
- Pulir animaciones (p. ej. la entrada de la sección de Completadas que se perdió) junto con otras
  mejoras, después de las fases.
- Conservar el plegado de los bloques de Tareas al cambiar de pestaña: el autor prefiere el
  comportamiento actual (con tareas → desplegado).
