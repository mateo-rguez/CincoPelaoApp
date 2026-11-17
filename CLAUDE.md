# CincoPelao

App Android de autogestión académica (asignaturas, eventos con notas, tareas con subtareas,
calendario, horario, recordatorios e intercambio de datos entre móviles por BLE + Wi‑Fi Direct).
Kotlin + Jetpack Compose + Room + Hilt + DataStore + AlarmManager.

El proyecto está en pleno refactor por fases: **el plan, lo hecho y lo pendiente están en
[`docs/REFACTOR.md`](docs/REFACTOR.md)**. Léelo antes de continuar.

## Trabajo

- Todo el refactor va en la rama `rework`; `master` es la versión antigua estable.
- Commits pequeños, uno por cambio coherente, con mensaje en español que explique el porqué y
  liste los bugs corregidos. El autor hace el push desde Android Studio (su clave SSH tiene
  passphrase): avisar cuando haya commits para subir.
- Responder y comentar el código en español.

## Compilar y probar

```bash
./gradlew assembleDebug            # compilar
./gradlew testDebugUnitTest        # tests (JUnit + Robolectric, en la JVM)
./gradlew installDebug             # instalar en el móvil conectado por USB
./gradlew compileDebugKotlin --rerun-tasks   # si la compilación incremental parece no recompilar
./gradlew clean                    # si Hilt falla con clases generadas antiguas tras mover paquetes
```

- adb: `~/Android/Sdk/platform-tools/adb`. Paquete: `io.github.mateorguez.cincopelao`.
- El móvil de pruebas es el personal del autor: **antes de automatizar pulsaciones, comprobar que
  Cinco Pelao está en primer plano**, no tocar sus datos (probar en una instancia de prueba y
  borrarla después) y restaurar cualquier ajuste del sistema que se cambie.

## Arquitectura (`app/src/main/java/io/github/mateorguez/cincopelao/`)

- `data/local` — Room: `AppDatabase` (una base de datos por instancia), `dao/`, `entity/`.
- `data/instances` — `InstanceRepository`: instancias, la activa y su base de datos.
- `data/repository` — `SubjectRepository`, `EventRepository`, `TaskRepository` (Flows que siguen
  a la instancia activa con `flatMapLatest`; operaciones compuestas en una transacción),
  `TaskMaintenance`, reglas puras (`TaskRules`, `DataIntegrity`).
- `data/prefs` — DataStore: tema, ajustes de recordatorios, estado del horario.
- `domain/stats` — cálculo de notas y estadísticas (puro, con tests).
- `notifications` — `ReminderPlanner` y `ReminderSync` (puros) + `ReminderManager`, que reconcilia
  con alarmas exactas de `AlarmManager` (`ReminderAlarms`, `ReminderReceiver`; no con WorkManager,
  que ColorOS congela con la app cerrada).
  **La UI nunca programa ni cancela recordatorios**: se sincronizan solos al cambiar los datos.
- `sharing` — compartición: `protocol` (tramas, mensajes, selección; puro), `session` (emisor/receptor,
  fases, permisos; puro), `ble` y `wifidirect` (transportes), `ShareEnvironment`. Cómo funciona por dentro:
  [`docs/COMPARTICION.md`](docs/COMPARTICION.md).
- `ui/<pantalla>` — una pantalla = `XxxScreen` + `XxxViewModel` con un `uiState: StateFlow`
  (`collectAsStateWithLifecycle`); componentes en archivos propios del mismo paquete.
  `ui/theme`: `CincoPelaoTheme`/`CincoPelaoAppTheme`, `LocalDarkTheme`, tipografía (`Type.kt`) y una
  paleta por archivo (`PaletteXxx.kt`, claro y oscuro con todos los colores de `ColorScheme` y
  `AppColors`); cómo añadir una, en `Palettes.kt`.
- `di` — módulos de Hilt y `@ApplicationScope`.

## Reglas

- **Room**: la base de datos está en la versión 8 y el esquema se exporta a `app/schemas/`.
  Cualquier cambio en una entidad necesita subir la versión y escribir una migración.
- Lógica fuera de los composables: si se puede probar sin Android, va en una función pura con test.
- Nada de `!!`, `CoroutineScope(...)` sueltos ni lecturas de DataStore/BD dentro de la UI.
- Colores: las pantallas solo usan `MaterialTheme.colorScheme` y `MaterialTheme.appColors`; nada
  de colores fijos ni `if (oscuro)` fuera de las paletas. Un color nuevo = una variable en
  `AppColors` con su valor en cada paleta y modo, y su comprobación en `PalettesTest`.
- Letra: las pantallas eligen un estilo de `MaterialTheme.typography` y no ponen `fontFamily` a
  mano. Los estilos display, headline, titleLarge y titleMedium son de título (la letra que elige
  el usuario en Apariencia); el resto, de texto (DM Sans).
