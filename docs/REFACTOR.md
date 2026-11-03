# Refactor de CincoPelao

Seguimiento del refactor de la app: plan por fases, lo hecho y lo que queda.
Rama de trabajo: `rework`. Última actualización: 3 de noviembre de 2026.

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
| 6 | Compartición BLE / Wi‑Fi Direct | ⏳ |
| 7 | Mejoras y funcionalidades nuevas | ⏳ |

**Siguiente paso:** fase 6. Se dejó en pausa porque probarla de verdad necesita **dos móviles
Android** (Wi‑Fi Direct no funciona entre dos apps del mismo teléfono). La lógica y el protocolo
se pueden reescribir y probar con tests sin ellos; la prueba de conexión real, no.

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

### Bugs corregidos más relevantes
- Crashes: editar una instancia ya borrada; `instanceId!!` en Horario; `subject!!` en el
  Calendario; estadísticas con un evento sin peso; franja horaria vacía en los recordatorios.
- Datos: deshacer borrados y "eliminar solo esta" podían perder subtareas o cerrar la app;
  importar tareas sin sus asignaturas fallaba en silencio; el guardado de una asignatura podía
  cancelarse; editar un evento nuevo modificaba otro.
- Recordatorios: nunca se pedía el permiso (no llegaba ninguno); eventos y tareas se pisaban;
  medianoche en UTC; avisos fantasma al borrar o completar.
- Interfaz: parpadeo del tema claro en modo oscuro; doble navegación en la barra inferior;
  formularios que perdían lo escrito al girar o al cambiar datos; selectores de fecha cuyo
  Cancelar no deshacía; "Completada el java.text.SimpleDateFormat@…"; carga de imágenes y PDF
  en el hilo principal.

Tests: 55 (JUnit + Robolectric con Room y WorkManager de pruebas) — `./gradlew testDebugUnitTest`.

## Pendiente

### Fase 5 (resto, se hará con la fase 6)
- [x] **Instancias**: pantalla separada de la Activity; ya no se reinicia la app al cambiar de instancia.
- [ ] Retirar `MainViewModel` (solo lo usa ya Transmitir; se hará junto con la fase 6).
- [x] Ids unificados a `Long` en todas las entidades (el esquema de Room es idéntico).

### Fase 6 — Compartición
- [ ] Reescribir BLE y Wi‑Fi Direct con un ciclo de vida limpio: el escaneo BLE no se detiene
      nunca, `stopServer` no para el anuncio y la desconexión se notifica con un id distinto al
      que se filtra.
- [ ] Los datos recibidos se acumulan en un singleton y la importación usa `messages[0]`:
      una segunda recepción importaría los datos de la primera.
- [ ] Protocolo con versión y límite de tamaño (ahora se fía del tamaño que llega).
- [ ] Avisar al usuario si la importación falla; no mostrar la contraseña del grupo en un Toast.
- [ ] Gestión de permisos (ahora se piden en bloque, incluido uno que no es de tiempo de ejecución).

### Fase 7 — Mejoras (propuestas)
- Widgets de próximos exámenes y tareas.
- Copia de seguridad: exportar e importar a archivo.
- Horario estructurado (no solo imagen) que pueda avisar de las clases.
- Media ponderada global por curso.
- Material You con colores dinámicos.
- Publicación: R8/minify activado y firma de release.

### Aplazado a propósito
- Actualizar versiones de librerías (Compose, Kotlin, AGP…): el autor prefirió no hacerlo aún.
- Textos de la interfaz a `strings.xml` (~340 textos): la app seguirá solo en español por ahora;
  se hará si se añade otro idioma.
- Pulir animaciones (p. ej. la entrada de la sección de Completadas que se perdió) junto con otras
  mejoras, después de las fases.
- Conservar el plegado de los bloques de Tareas al cambiar de pestaña: el autor prefiere el
  comportamiento actual (con tareas → desplegado).
