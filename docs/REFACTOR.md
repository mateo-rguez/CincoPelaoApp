# Refactor de CincoPelao

Seguimiento del refactor de la app: plan por fases, lo hecho y lo que queda.
Rama de trabajo: `rework`. Última actualización: 17 de noviembre de 2026 (fase 7: editar/eliminar dentro de la tarjeta de asignatura, ayuda de la hoja de evento y recordatorios con alarmas en vez de WorkManager).

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
| 4 | Recordatorios: planificador puro + reconciliación (con WorkManager; desde la fase 7, con alarmas) | ✅ |
| 5 | Interfaz: tema, navegación, un ViewModel por pantalla, rendimiento | ✅ |
| 6 | Compartición BLE / Wi‑Fi Direct | ✅ (probada con dos móviles) |
| 7 | Mejoras, una a una (lista abierta) | ⏳ |

**Siguiente paso:** la fase 7 no es un bloque, sino una lista de mejoras que se amplía y se
aplica de una en una (ver «Fase 7 — Mejoras»). Hechas: **colores y temas** y **pulir la
interfaz**, pantalla por pantalla con el autor: Tareas y Asignaturas (lista, detalle y
estadísticas), Calendario, Horario, Notificaciones, Instancias, Compartir y el menú están
terminadas (Apariencia se queda como está), y también la pantalla de crear/editar asignatura.
El 17 de noviembre se hicieron también editar/eliminar dentro de la tarjeta de asignatura, la
ayuda fija bajo ponderación y nota mínima y los recordatorios con alarmas (llegaban solo al abrir
la app); los eventos del formulario en tarjetas se probaron y se descartaron. Después, la
ilustración de Asignaturas vacía, las animaciones (pestañas y listas; cerradas) y la revisión de
temas (paletas escritas enteras y colores de nota por paleta). Los recordatorios con alarmas
ya se han comprobado en el Realme. **En curso: tipografía** (texto en DM Sans y letra de los
títulos a elegir en Apariencia) y **más paletas** (Catppuccin, Rosé Pine y Gruvbox; falta que el
autor las pruebe). **Después:** el resto de la lista (preferencias, medir arranque y scroll...). Método usado en cada pantalla: capturas con la app en
primer plano, 3–5 propuestas concretas, el autor elige, se instala, se prueba y se ajusta. La fase 6 está probada por el autor con un Realme
(Android 16) y un Samsung Galaxy A56 (Android 17): todas las pruebas de la lista pasan.

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

Tests: 206, todos en verde a 17 de noviembre de 2026 (JUnit + Robolectric con Android 15, Room, WorkManager y AlarmManager de pruebas, y sockets TCP locales para la
compartición) — `./gradlew testDebugUnitTest`.

## Pendiente

### Pruebas con dos móviles (fase 6)

Resultado (8 de noviembre de 2026): todas correctas en Realme (Android 16) + Samsung A56
(Android 17). La 12 en Android 10–12 queda descartada (no hay móviles con esas versiones).
Sirven de lista de regresión si se toca la compartición.

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
12. ~~**Otras marcas** y Android 10–12~~ (allí se pide ubicación): descartada, no hay móviles
    con esas versiones.

### Fase 7 — Mejoras (una a una)

Lista abierta: se amplía cuando surge algo y cada mejora se hace por separado, con sus commits.
Estado: ⏳ en curso · ☐ pendiente · ✅ hecha.

- ✅ **Colores y temas** (pedido por el autor el 8 de noviembre de 2026, hecho el 10):
  - `67250ab` Modo Sistema / Claro / Oscuro y paleta en DataStore (`ThemeSettings`, con tests);
    el interruptor antiguo `dark_mode` se migra solo.
  - `04e2d3d` `AppColors` (`MaterialTheme.appColors`): colores con significado por paleta en vez de
    `if (oscuro) Blanco else primary` en cada pantalla; arreglados contrastes de Clásico oscuro.
  - `f635643` Paletas Bosque, Coral, Violeta y Grafito, con test de contraste (WCAG AA).
    Comprueba el texto, las barras, el acento y las tarjetas de la línea de tiempo.
  - `d460653` Pantalla «Apariencia» (menú lateral) con miniaturas de cada paleta.
  - `bdc889a` Vuelve el interruptor de modo oscuro en el menú, junto a Apariencia (el autor lo
    prefiere como atajo).
  - Tras revisarlo el autor: Clásico oscuro vuelve al texto blanco (la compartición conserva su
    lila), los puntos de la línea de tiempo siguen a la paleta, y se quitan Bosque, Coral y
    Violeta (`322da0e`). Quedan **Clásico y Grafito**.
  - `8ce8a95` Grafito: eventos en azul acero y tareas en verde azulado en la línea de tiempo, con tarjetas
    más claras en oscuro y puntos que no se confunden con la línea gris.
  - Para añadir una paleta: un valor en `AppPalette`, su nombre en `displayName` y dos
    `namedPalette(...)` (claro y oscuro) en `ui/theme/Palettes.kt`; el test de contraste la revisa.
  - Visto y sin tocar: los días de otros meses del calendario (`CalendarInactiveText`, gris fijo)
    se leen poco sobre fondos oscuros.
- ⏳ **Más paletas** (17 de noviembre de 2026): el autor eligió Catppuccin, Rosé Pine y Gruvbox
  (de la comunidad Linux, licencia MIT) tras una comparativa con maquetas en claro y oscuro.
  - `PaletteCatppuccin.kt` (Latte / Mocha, barra y acento mauve en claro), `PaletteRosePine.kt`
    (Dawn / Moon, barra pino y acento rosa en claro) y `PaletteGruvbox.kt` (Light / Dark, barra
    marrón y acento naranja): todos los colores salen de los tonos oficiales o de mezclas suyas.
  - Los tonos de texto que no llegaban al contraste de `PalettesTest` se oscurecieron (claro) o
    aclararon (oscuro) lo justo; sobre todo amarillos y naranjas en claro, como ya pasaba en Grafito.
  - Pendiente: que el autor las pruebe en el móvil y ajustar lo que no le guste.
- ✅ **Calendario: tarea o evento en cada tarjeta**: hecho dentro de «Pulir la interfaz» (etiqueta
  del tipo y marcador con forma).
- ✅ **Pulir la interfaz**: hacer algunas pantallas más amigables y bonitas. Se concreta
  pantalla por pantalla con el autor (pedido el 10 de noviembre de 2026). Orden acordado:
  Tareas ✅ → Asignaturas (lista, detalle, estadísticas) ✅ → Calendario (aquí entra «tarea o
  evento en cada tarjeta») ✅ → Horario, Notificaciones, Instancias ✅ → Compartir ✅ → menú y
  Apariencia ✅.
  - **Tareas** ✅:
    - `8985f30` Tarjetas rellenas (`appColors.taskCard`) con sombra suave teñida por la paleta y
      brillo en el borde; fecha límite como etiqueta con color según la urgencia (`dueUrgency`):
      roja si venció, naranja hoy o mañana. «Venció ayer / hace N días» en vez de «Para hace N
      días». Completadas usa la misma tarjeta; su emoji de vacío se queda como está (lo pidió el autor).
    - `6b10402` Arreglo surgido al probar: en Grafito oscuro los botones del selector de fecha no
      se veían. `colorScheme.primary` hacía de color de barras y de acento de los componentes de
      Material; ahora las barras usan `appColors.bar`/`onBar` y, en oscuro, `primary` es un tono
      legible (azul claro `#9AA6FF` en Clásico, el cian en Grafito) con texto oscuro. El autor
      rechazó el blanco en Clásico oscuro (botones rellenos blancos, feos). El test de contraste
      revisa los controles sobre diálogos (`surfaceContainerHigh`) y fondo.
    - `b348105` Casilla redonda (`TaskCheckbox`), barra de progreso «2/5» en tareas con
      subtareas, separadores suaves de ancho completo bajo la madre y entre subtareas (el autor
      los quiere), cabeceras con pastilla de pendientes (`pendingCount`) e iconos a la derecha;
      si el nombre no cabe se muestra la abreviatura (`groupTitle`).
    - Sin tocar: la casilla de la ficha de una tarea (`TaskInfoSheet`) sigue siendo la cuadrada
      de Material; el formulario de tarea no se ha revisado.
  - **Asignaturas** ✅:
    - `eaf0f83` Lista: tarjeta con sombra de Tareas, pastilla con abreviatura (o iniciales) y
      una fila con dos etiquetas que alternan cada 5 s a la vez en todas: nota (media ↔ último
      examen; nota final fija en color de acento si ya está todo hecho) y lo siguiente (próximo
      evento ↔ tareas pendientes, o «Todo al día»). Datos en `subjectSummaries` (puro, con tests).
    - `50fdc50` Arreglo: al cambiar de instancia la lista salía desplazada hasta abajo (claves
      por id repetidas entre BD); las pestañas se recomponen desde cero al cambiar de instancia.
    - `3fff31e` Editar/eliminar en un menú «⋮» pequeño en la esquina (el autor lo eligió frente a
      pulsación larga con menú o deslizar); fuera la barra desplegable. Interlineado del nombre
      ajustado para que el hueco entre sus líneas sea el mismo que hasta el curso.
    - `5b12387` Detalle: eventos en filas con separador (sin tarjetas, a propósito) y tres franjas
      alineadas (nombre ↔ flechas de subir, etiquetas ↔ nota, estado ↔ flechas de bajar). Se
      conservan las tres flechas ±1/±0,1/±0,01 (al autor le parecen muy cómodas). El estado dice
      cuánto falta mientras no hay nota. Bajar desde vacío ya no pone 0,00.
    - La cabecera del detalle (número grande acumulado, brillo, frase con la media) se queda
      como está, por decisión del autor.
    - Estadísticas (`SubjectStatsScreen`):
      - `43ddf87` Limpieza: la gráfica tenía dos copias del dibujo; fuera `!!` y `Color.Red`;
        etiquetas de regularidad y tendencia puras (`StatsUtils`).
      - `3458d0b` Calculadora: slider de medio en medio punto (el autor lo prefiere a flechas por
        sus puntos), frase de resumen (`targetVerdict`) y filas compactas con la nota necesaria
        coloreada y una palabra en la misma línea (`requirement`: Asequible hasta 7, Exigente,
        Imposible, Basta la mínima). Arreglado «(Completado) 8.379999».
      - `71d95f7` Datos en bloques (Progreso, Nota final, Tus notas, Reparto) en lugar de 21
        filas. Fuera eventos pendientes, % de eventos completados, mediana y rango; se conservan
        nota ponderada y promedio (el autor los quiere aquí aunque estén en la cabecera). Casillas
        en orden «con 5 en todo · si sigues así · máximo posible» (una sola si no queda nada).
        Datos nuevos en `subjectInsights`. Títulos y separaciones comunes en toda la pantalla.
      - `1202c28` Gráfica (`GradeChart.kt`): nombres en horizontal alternados en dos alturas (se
        probaron girados y sin márgenes: el autor prefirió esto), nota sobre cada punto,
        pendientes huecos, línea en el 5, puntos suspensos (menos de 5 o bajo la mínima) en rojo.
      - `d5111f6` Altura final 270 dp (se probaron 300 y 240) y línea de un solo color: el degradado
        de la línea hacia los suspensos se probó y no gustó.
  - **Calendario** ✅ (11–15 de noviembre de 2026). El autor eligió: tarjetas más claras, cabeceras
    relativas, puntos por tipo en la cuadrícula y hoja del día con tarjetas; además, rediseñar la
    línea de tiempo entera (línea, puntos, fechas) con formas de Material 3.
    - ✅ Línea de tiempo propia (`Timeline.kt`), probada en el móvil y ajustada con el autor
      (13 de noviembre): `8edf293` color de los exámenes, `530e7dc` fechas relativas y
      cabecera, `90f1ec3` el árbol. Fuera JetLime (no dejaba cambiar los puntos ni la colocación de las
      fechas). Es un árbol: un tronco común, opaco, pasa por un punto en cada día con la fecha
      a 18 en negrita («Martes 17 nov · En 4 días», `dayHeaderLabel` y `relativeDayLabel`) y de
      él sale una rama curva, del mismo color, hasta el borde de la tarjeta de cada elemento.
      Tarjetas con el fondo de Tareas (`TaskCardFrame`): etiqueta de color con el tipo y la
      modalidad, nombre a 16 y «SIGLAS · asignatura» a 12. Los exámenes tienen color propio
      (`timelineExamPoint`, rosa en las dos paletas). Las fechas lejanas van en semanas («En 3
      sem.») desde 14 días y en meses desde 60. Lógica pura en `CalendarViewModel.kt`
      (`TimelineDay`, `relativeDayLabel`, `dayHeaderLabel`, `marker`, `typeLabel`,
      `subjectLine`) con tests.
    - Probado y descartado por el autor, para no repetirlo: la fecha en una columna a la
      izquierda de la línea; un árbol separado por día; marcadores con forma en cada elemento
      (estallido, pentágono, galleta, aro; con `androidx.graphics:graphics-shapes`) y también
      en el punto de cada día girando despacio; ramas teñidas con el color del elemento; la
      cabecera en mayúsculas; tarjetas con el fondo teñido según el tipo y el título arriba.
    - ✅ `9f4fffa` Fuera `timelineEventContainer` / `timelineTaskContainer` (fondos de las
      tarjetas de JetLime); la miniatura de Apariencia usa el color de las tareas.
    - ✅ `26fda6e` Contraste de los tipos: el test comprueba ahora la etiqueta de cada tipo sobre
      la tarjeta (4,5:1) y los puntos sobre el fondo y la casilla (3:1). Clásico tiene tonos
      propios en claro y en oscuro, y su casilla ya no es `Color.Cyan`/`Blue`/`Black`.
    - ✅ `bfce1a6` Cuadrícula con un punto del color de su tipo por cada elemento del día (hasta
      4 y «+»; el autor lo prefirió a un punto por tipo, a una barra o a teñir la celda) y días
      de otros meses con el texto de la paleta atenuado (fuera `CalendarInactiveText`). Todo
      sale de `itemsByDate` (también lo pasado).
    - ✅ `bfce1a6` Hoja del día en filas con separador, no tarjetas (lo eligió el autor): punto de
      color, nombre y «Entregable · Prácticas · SMM» (`shortLine`); título como en la línea de
      tiempo, con «Ayer» / «Hace 9 días» para días pasados, y «Día libre» si no hay nada.
    - ✅ `86e2356` Calendario propio con `HorizontalPager` (fuera la librería de Kizitonwose):
      celdas redondeadas, hoy con borde de acento, día elegido con relleno suave. Siempre 6
      semanas (el autor no quería que cambiase de alto); el relleno se reparte: un mes de 5
      semanas añade una al final y uno de 4, una antes y otra después (`monthGridDays`).
    - Nota: el móvil rechaza instalar desde un PC distinto (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`:
      cada PC tiene su `~/.android/debug.keystore`). No desinstalar (se pierden los datos): copiar
      la misma `debug.keystore` en los dos PCs o instalar desde el PC que firmó la versión instalada.
  - **Horario** ✅ (15 de noviembre de 2026), probado con una instancia de prueba con y sin
    horario (borrada después):
    - `e6fccc8` Zoom: el desplazamiento sigue al dedo 1:1 (antes se dividía por el zoom y a 10×
      iba al 25 %, el autor lo notaba lento y tosco); pellizco hacia los dedos y doble toque
      animado hacia el punto tocado; sin arrastrar más allá de los bordes (`ZoomMath.kt`, con
      tests). Vacío centrado con el reloj en un círculo suave. Botón «⋮» que se transforma en una
      tarjeta (Cambiar archivo / Quitar horario) y esta en la confirmación: el autor no quería un
      menú desplegable normal. `FloatingActionButtonMenu` de Material 3 llega en la 1.4 (tenemos
      la 1.3): si se actualizan las librerías, valorar usarlo. Antes no se podía quitar el horario.

  - **Notificaciones** ✅ (15 de noviembre de 2026):
    - `ec0a6c4` Frase resumen en una tarjeta con campana y días/horas resaltados
      (`reminderSummaryParts`); días como pastillas en rejilla de 4 columnas (1, 2, 3, 5, 7, 14 y
      «+ Otro»), que el autor eligió frente a filas o una línea de tiempo; franja con un slider de
      dos tiradores y las horas en grande (fuera el `TimePickerDialog` antiguo); guardado al
      momento, sin botón. Franja por defecto 8:00–24:00 (solo instalaciones nuevas). Tamaños
      ajustados con el autor: primero se quedó corto y luego grande; pastillas de 40 dp y horas
      en `headlineSmall` le encajan.
  - **Horario**, arreglo (16 de noviembre de 2026): `1ace6f4` la primera visita a la pestaña tras
    abrir la app salía vacía y a tirones (el PDF se renderizaba a 3× durante la animación de
    entrada); ahora `ScheduleImageCache` lo precarga al abrir la app y `prepareToDraw()` lo sube a
    la GPU antes de pintarlo.
  - **Instancias** ✅ (16 de noviembre de 2026):
    - `b5f1f3b` Resumen en cada tarjeta («Teleco · 10 asignaturas · 5 tareas · horario») y
      pastilla «✓ Activa»; fuera «Cancelar»; «＋ Nueva instancia» como última fila con borde
      discontinuo; crear y editar transformando la tarjeta en el formulario (titulación también
      al crear) con «Eliminar» y confirmación en la misma tarjeta. Deslizar: la tarjeta vuelve y
      se convierte en la confirmación, fondo de gris a rojo con papelera que crece y vibra al
      pasar el umbral, la activa explica que no se puede borrar, y una pista bajo la lista (el
      autor quería que se descubriera). Al confirmar se pide el desbloqueo del móvil
      (`DeviceAuth.kt`, `BiometricPrompt` del sistema, sin dependencias). El autor eligió
      tarjetas con resumen frente a filas con divisores.
  - **Compartir** ✅ (16 de noviembre de 2026): Transmitir y Recibir ya estaban modernizadas
    (fase 6), así que se pulió la pestaña previa y, de paso, los vacíos:
    - `8da4fb5` `EmptyState` común (`ui/common`) para el Horario sin horario, Asignaturas sin
      asignaturas y la pestaña de compartir: círculo suave con la ilustración, título, texto y
      acciones; ondas opcionales. Compartir: logo con ondas, qué se comparte y «No hace falta
      conexión a internet» (el autor no quería nombrar Bluetooth ni Wi‑Fi Direct aquí), tarjeta
      con Transmitir/Recibir explicadas y «Tus notas y tu progreso no se envían». Asignaturas
      vacía: «Añadir asignatura» y «Recibir de un compañero» (lleva a la pestaña de compartir)
      igual de anchos, sin el «+» flotante; sin parpadeo del aviso al cargar. Recibir: contador
      del nombre en «caracteres» (las tildes y la ñ cuentan doble). El autor prefirió no
      renombrar las pestañas «ShareMode» y «Materias».
  - **Menú y Apariencia** ✅ (16 de noviembre de 2026):
    - `e679270` Menú lateral en su archivo (`AppDrawer.kt`): título «Menú», las cinco pestañas y,
      tras un divisor, Modo oscuro, Apariencia, Instancias y Notificaciones, todas con la misma
      fila (48 dp, icono 22 dp, texto 15 sp; se probó a 56 dp y le pareció grande). Pie con
      «Cinco Pelao · versión X» y el logo de GitHub con `mateo-rguez`, sin arroba ni enlace.
      Se probó a quitar las pestañas (están en la barra inferior), con «Autogestión académica»
      y «Hecho por @…» enlazado: el autor prefirió esto.
    - Apariencia se queda como está (decisión del autor). Siguen sin tocar el icono `[ ]` de
      Instancias y el pulgar negro del interruptor.

- ☐ **Pantalla de preferencias** (idea del autor, 11 de noviembre de 2026): entre otras cosas,
  elegir qué muestra el número grande de la cabecera de una asignatura (nota acumulada o media).
- ☐ **Rendimiento del scroll** (notado por el autor el 11 de noviembre de 2026): la app antigua
  daba tirones al hacer scroll rápido arriba y abajo, sobre todo en el Realme (en el Samsung
  bastante menos). Tras las primeras fases casi desapareció, pero con el pulido de la interfaz
  quizá vuelve a notarse un poco. Medir antes de tocar nada, en todas las listas (Tareas,
  Completadas, Asignaturas, eventos, calendario):
  - comparar con una compilación **release** (la debug de Compose es bastante más lenta y no
    sirve para juzgar) y, si hace falta, añadir un *baseline profile*;
  - fotogramas lentos con `adb shell dumpsys gfxinfo io.github.mateorguez.cincopelao` tras
    hacer scroll, o con Macrobenchmark si se quiere automatizar;
  - sospechosos a revisar: sombras teñidas de `TaskCardFrame` (`shadow` + borde con degradado
    en cada tarjeta), el reloj de 5 s que recompone todas las tarjetas de Asignaturas, y
    claves/`contentType` en las `LazyColumn`.
- ✅ **Ilustración de Asignaturas vacía** (16–17 de noviembre de 2026): el icono de apuntes con
  lápiz era provisional (tampoco gustaron unos libros apilados dibujados a trazo). Ahora es un
  archivador abierto con pestañas vacías bajo un flexo, generado con IA en un solo color:
  blanco sobre transparente en `res/drawable-nodpi/empty_subjects.png` (560 px, solo alfa,
  34 KB), dibujado con `Icon` para que se tiña con el color del texto de la paleta. A 145 dp
  dentro del círculo de 180 dp del `EmptyState` (a 120 dp no se distinguían los anillos).
  El original venía como SVG con un PNG dentro, no vectorial; si algún día se quiere en
  VectorDrawable, hace falta vectorizarlo (potrace o similar, no instalado).
- ✅ **Pulir la pantalla de crear/editar asignatura** (17 de noviembre de 2026):
  - `0107388` Arriba, la tarjeta de la lista como vista previa en vivo, que se despliega en
    nombre, abreviatura (sugerida desde el nombre con `suggestAbbrev` hasta que se escribe a
    mano) y curso en pastillas; se pliega y suelta el foco al añadir o abrir un evento. Eventos
    en filas con divisores (nombre; tipo · modalidad · fecha corta · mín. · nota; ponderación a
    la derecha) y medidor de ponderación con barra y «Faltan/Sobran X %» en su propia línea.
    «Guardar» en la barra, aviso de descartar cambios al salir, errores en cada campo.
    `EventSheet` sustituye a `EventDialog` (también en el detalle): pastillas de tipo y
    modalidad, interruptor «Cuenta para la nota», nota mínima sobre 10 (se sigue guardando en
    %), eliminar con deshacer; arrastrar/tocar fuera/atrás/Cancelar con algo escrito piden
    confirmación (el atrás de la hoja lo gestiona un `BackHandler` propio porque en Android 13+
    el de `ModalBottomSheet` no consulta `confirmValueChange`). Pastilla común `ChoicePill` en
    `ui/common`. Reglas puras en `SubjectForm.kt` con `SubjectFormTest`.
    Corregido: «8.379999» en vez de «8,38», el «Guardar» flotante tapaba el último evento,
    atrás perdía lo escrito, el teclado volvía tras elegir la fecha (el foco de la hoja es de
    otra ventana: hay que usar el `LocalFocusManager` de dentro).
  - El autor pidió quitar el «Faltan X %» de debajo de la ponderación en la hoja (sí lo quiere
    en el medidor de la lista de eventos).
- ✖ **Eventos del formulario en tarjetas** (idea del autor, probada y descartada el 17 de
  noviembre de 2026): con tarjetas la pantalla quedaba recargada («ya es mucha tarjeta en esta
  pantalla y en toda la app»); se quedan las filas con divisores.
- ✅ **Menú de editar/eliminar asignatura** (idea del autor, 17 de noviembre de 2026): el «⋮»
  de `SubjectCard` ya no abre un menú flotante, sino que despliega dentro de la tarjeta (con el
  muelle de Instancias) una franja con divisor y Editar · Eliminar; el «⋮» pasa a «✕». Eliminar
  se confirma en la misma franja y pide el desbloqueo del móvil (`requestDeviceAuth`, movida a
  `ui/common/DeviceAuth.kt` para compartirla con Instancias); después sigue el «Deshacer».
  Solo una tarjeta abierta a la vez; atrás o tocar la tarjeta la cierra.
- ✅ **«Opcional» bajo la nota mínima** (17 de noviembre de 2026): en la hoja de evento las dos
  casillas llevan siempre su ayuda («De 1 a 100» y «Opcional · 0 a 10»), así quedan iguales y,
  con un error, el mismo texto se pone en rojo sin que la hoja salte. Hechos en `7ef1c0a`:
  editar/eliminar dentro de la tarjeta de asignatura.
- ✅ **Revisar la hora de las notificaciones** (notado por el autor el 16 de noviembre de 2026;
  resuelto el 17): los avisos solo llegaban al abrir la app. Comprobado en el Realme: las tres
  notificaciones que guardaba `dumpsys usagestats` saltaron un segundo después de abrirla, y las dos
  que se pudieron cruzar con la BD de WorkManager debían haber saltado 13 min y 1,5 h antes. La
  hora sorteada era correcta; ColorOS congela el JobScheduler (y con él WorkManager) con la app
  cerrada y lo suelta todo al volver a primer plano.
  - Ahora los recordatorios son alarmas de `AlarmManager` (`ReminderAlarms`): exactas
    (`SCHEDULE_EXACT_ALARM` en Android 12, `USE_EXACT_ALARM` desde 13, se conceden al instalar;
    no hay pega fuera de Google Play) y, si no se puede, inexactas. `ReminderReceiver` muestra el
    aviso; `ReminderBootReceiver` las vuelve a poner al reiniciar, actualizar o cambiar el permiso.
  - Las horas sorteadas se guardan en un DataStore propio (`reminder_alarms`) en vez de en las
    etiquetas de WorkManager; la reconciliación es pura (`ReminderSync`, con tests). Migración
    única: se conservan las horas ya sorteadas y los avisos que ya saltaron no se repiten.
  - ColorOS da a las alarmas exactas de casi todas las apps (WhatsApp y Google Play Services
    incluidos; Google Calendar no) una ventana de 1 h (`dumpsys alarm`: `window=+1h`): pueden
    llegar hasta una hora tarde, pero ya no esperan a que se abra la app. Si molesta, siguiente
    paso: pedir la exención de la optimización de batería (descartado por ahora por el autor).
  - Por el camino: Robolectric usaba Android 5.0 al no leer el manifiesto; se fija Android 15 en
    `app/src/test/resources/robolectric.properties`.
  - Commits: `7ef1c0a` (editar/eliminar en la tarjeta), `2d99eeb` (ayuda de la hoja de evento),
    `8cd622b` (tarjetas descartadas) y el de este cambio.
  - Comprobado en el Realme el 17 de noviembre: el aviso saltó a las 19:07:18 con la app cerrada
    (`dumpsys usagestats`: última apertura a las 18:59, la siguiente a las 19:34). La hora no
    coincide con la apuntada porque se volvió a sortear al abrir la app a las 18:58; las alarmas
    siguientes aparecen en `dumpsys alarm` como exactas (`exactAllowReason=policy_permission`).
- ☐ **Medir el arranque** (pedido por el autor el 16 de noviembre de 2026; abre rápido y no
  parece preocupante, pero conviene tener el dato): tiempo hasta el primer fotograma en frío
  con `adb shell am start -W -S io.github.mateorguez.cincopelao/.MainActivity` (`TotalTime`),
  en **release** y en los dos móviles. Ojo: desde el 16 de noviembre se precarga el horario al
  arrancar (`ScheduleImageCache`); va en segundo plano, pero comprobar que no retrasa el inicio.
- ✅ **Revisar que los temas estén limpios** (pedido por el autor el 11 de noviembre de 2026;
  hecho el 17): objetivo del autor, una paleta nueva = apuntar sus colores en cada variable, sin
  apaños fuera de las paletas ni `if (oscuro)`.
  - `812924d` Cada paleta es un archivo (`PaletteClassic.kt`, `PaletteGraphite.kt`) con sus dos
    modos escritos enteros: los 36 colores del `ColorScheme` y todos los de `AppColors`, que ya
    no tiene valores por defecto. Antes Clásico solo fijaba 7 colores de Material y heredaba el
    resto de la paleta base morada sin decirlo, y Grafito se montaba con `namedPalette`, que
    deducía colores con `if (dark)` y a partir de otros del esquema. Sin cambios visibles: los
    236 colores se volcaron antes y después y son idénticos. `Palettes.kt` explica cómo añadir
    una paleta; `Color.kt` solo conserva la consola de Transmitir/Recibir (oscura a propósito).
  - `09fa054` Colores de nota, «hecho» y texto apagado propios de cada paleta y modo
    (`gradeBelowMinimum`…`gradeExcellent`, `done`, `muted`), en vez de constantes fijas y
    `Color.Gray`. Las etiquetas de los eventos pasan a texto del tono sobre ese tono suave
    (como las de fecha), porque con texto blanco «Aceptable» tenía contraste 2,0 y en oscuro
    «Nota mínima» como texto 1,6. `PalettesTest` comprueba todos (4,5:1) en cada paleta y modo.
    El autor probó el fondo de las etiquetas al 28 % y prefirió el 15 % de las fechas.
  - Queda a propósito: divisores, bordes, rellenos tenues y textos secundarios hechos con
    transparencia sobre `onBackground`/`onSurface` (se adaptan solos a cualquier paleta). Mejora
    opcional: unificarlos en unos pocos roles con nombre (hoy hay divisores al 12 % y al 50 % y
    textos secundarios al 45–75 % sin comprobar su contraste).
- ⏳ **Tipografía** (17 de noviembre de 2026): la letra se veía distinta en el Realme y en el
  Samsung porque casi todo usaba la del sistema (en el Realme, OPlus Sans de ColorOS, que no se
  puede incluir) y solo unos 35 textos llevaban Delight puesta a mano. Elegido por el autor tras
  una comparativa: el texto, fijo; la letra de los títulos, a elegir en Apariencia.
  - Dos papeles en `ui/theme/Type.kt`: **texto** en DM Sans (body, label y titleSmall, que Material
    usa en pestañas) y **títulos** (display, headline, titleLarge y titleMedium, en peso medio)
    con la letra de `TitleFont`: Delight (por defecto), Plus Jakarta Sans, Source Serif 4 o
    Fraunces. Guardada en DataStore (`theme_title_font`) junto al tema; `appTypography` y
    `TypographyTest`.
  - Fuentes de Google (OFL, licencias en `assets/licencias`) como instancias estáticas de cada
    peso (400–700) recortadas al alfabeto latino: 1,4 MB en total. Fuera Lora, que no se usaba.
  - Las pantallas ya no ponen `fontFamily` a mano (salvo la monoespaciada de Compartir): el estilo
    decide. Los nombres de tareas y de elementos de la línea de tiempo pasan a `bodyLarge` (son
    texto, no títulos); los nombres de asignatura conservan el peso normal que tenían.
  - Pendiente: que el autor pruebe las cuatro letras en el móvil; mirar si DM Sans pide menos
    espaciado entre letras que el de Material (pensado para Roboto) y si la consola de Compartir
    necesita una monoespaciada propia.
  Ojo al compararlos: el Samsung tiene la letra del sistema al 90 % y el Realme al 100 % con el
  tamaño de pantalla ampliado (densidad 480 en vez de 560), así que parte de la diferencia de
  tamaño (p. ej. la tarjeta de la petición en Transmitir) viene de los ajustes de cada móvil.
- ✅ **Pulir animaciones** (17 de noviembre de 2026; cerrada por el autor tras A1 y C, sin B1). Orden acordado: A1 pestañas → C1
  `animateItem()` en las listas → C2 tareas que se tachan y pliegan / C3 tarjeta de asignatura
  que se encoge al borrarla (y la entrada de la sección de Completadas que se perdió) → B2
  animación propia al abrir pantallas + atrás predictivo → B1 tarjeta que se transforma en la
  pantalla de detalle (obliga a pasar el detalle y el formulario de *Activities* al `NavHost`).
  Opcional: entrada escalonada de las tarjetas solo al arrancar.
  - ✅ A1: entre pestañas, «eje compartido» horizontal (`ui/navigation/TabTransitions.kt`): la
    nueva entra desde el lado en que está en la barra, desplazando 1/6 del ancho; la anterior
    se desvanece en los primeros 110 ms de 320. Calendario sin su fundido propio de 600 ms.
    Sentido en `tabDirection` (puro, con test). Antes caían desde arriba a toda altura.
  - ✅ Arreglado por el camino: al cambiar de pestaña rápido, a veces la pantalla aparecía
    bajando desde arriba. El `AnimatedContent` del `NavHost` 2.7 lleva `SizeTransform` y centra
    el contenido; Asignaturas no dibuja nada hasta leer la BD (mide 0), así que el hueco crecía
    desde 0 y la lista, centrada y recortada, bajaba hasta su sitio. Cada pestaña va ahora en
    una caja a pantalla completa (`tab()` en `AppScaffold`).
  - El autor nota algún tirón al cambiar de pestaña: medirlo con lo del rendimiento (en
    release; la debug de Compose es bastante más lenta).
  - ✅ C1 `animateItem()` en Asignaturas, Instancias (también «Nueva instancia» y la pista) y
    eventos del detalle (también el título «No puntuables», ahora con clave): al añadir, borrar
    o reordenar, el resto se recoloca deslizándose.
  - ✅ C2 Tareas: no están en una lista *lazy* sino en columnas dentro de cada bloque, así que
    `ui/common/AnimatedColumnItems` (con test del orden, `keepRemovedInPlace`) despliega las
    tareas nuevas y pliega las que se archivan o borran, conservándolas en su sitio mientras
    salen; también las subtareas dentro de su madre (divisor encima de cada una salvo la
    primera, para que se pliegue con ella). La pastilla de pendientes cambia de número con un
    deslizamiento vertical (entra desde abajo si sube) y aparece/desaparece con zoom.
  - ✅ C3 Borrar asignatura: tras confirmar, la tarjeta se encoge al 85 %, se desvanece y se
    pliega, y solo entonces se borra de la BD; con «Deshacer» reaparece.
  - ✗ C4 Entrada de Completadas (las tarjetas desplegándose al abrirla, como en la versión
    antigua): probada y descartada, no le gustó al autor.
  - ✗ B2 Animación propia al abrir y cerrar pantallas (`windowAnimationStyle` en el tema: la
    nueva entraba desde la derecha desplazando un 12 % con fundido, la de debajo quieta) junto
    con el atrás predictivo (`enableOnBackInvokedCallback`): probada y descartada, al autor no
    le terminó de convencer; se quedan las animaciones del sistema.
  - B1 (tarjeta que se transforma en el detalle): no se hace, el autor da las animaciones por
    terminadas.
- ☐ Widgets de próximos exámenes y tareas.
- ☐ Copia de seguridad: exportar e importar a archivo.
- ☐ Horario estructurado (no solo imagen) que pueda avisar de las clases.
- ☐ Media ponderada global por curso.
- ☐ Material You con colores dinámicos (encajaría como un tema más).
- ☐ Publicación: R8/minify activado y firma de release.
- ☐ Animaciones Lottie para los iconos de compartir (si el autor encuentra unas que le gusten:
  basta con cambiar `ShareStatusIcon`).

### Aplazado a propósito
- Actualizar versiones de librerías (Compose, Kotlin, AGP…): el autor prefirió no hacerlo aún.
- Textos de la interfaz a `strings.xml` (~340 textos): la app seguirá solo en español por ahora;
  se hará si se añade otro idioma.
- Conservar el plegado de los bloques de Tareas al cambiar de pestaña: el autor prefiere el
  comportamiento actual (con tareas → desplegado).
