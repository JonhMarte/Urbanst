# GestorTrainer — contexto para Claude Code

Lee esto antes de tocar nada. Responde siempre en **español**. El usuario es Jonathan (Caracolas Games), ingeniero de sistemas; trabaja con Unity, Blender y AWS.

## Qué es

App Android (Kotlin + Jetpack Compose) que conecta a entrenadores (trainers) de cualquier disciplina con sus alumnos:
- El trainer crea **actividades** (ADD New Activity), vincula **alumnos por ID** (ADD New User) y les asigna actividades en un **calendario**.
- El alumno ve su **plan semanal**, **activa y desactiva** cada actividad, sube fotos y suma **puntos**.
- Hay **clasificaciones**: alumnos de cada trainer, usuarios globales y trainers globales (estos ganan visibilidad por méritos).
- Modelo de negocio: comisión por transacción, publicidad (solo plan gratuito del alumno) y servicios directos.

## Documentos de referencia (carpeta padre)

- `../ARQUITECTURA.md` — arquitectura del MVP v2 (módulos, interfaces, backend AWS, pagos, puntuación, privacidad, plan por fases). **Es la fuente de verdad.**
- `../Diseños/` — código de las 9 pantallas del MVP (`.dc.html`) y `LEEME.md`. Se ven en el canvas: https://claude.ai/artifact/S8bAHprcfii3tt6PVckJRT
- `../Documento de app gestora para.docx` — documento funcional original de Jonathan.
- Arquitectura en línea (misma que ARQUITECTURA.md): https://claude.ai/artifact/PPTTZyWLcArsXiLBSWWKug

## Decisiones tomadas (no reabrir sin preguntar)

- **Sin VR ni visores AR.** Todo desde el teléfono Android.
- **3D/AR fuera del MVP**, pero con punto de integración listo: `ExerciseMediaViewer` + `MediaViewerRegistry` (`feature/media`). El futuro `integrations/viewer3d` (Unity as a Library) añadirá otro visor con `@Binds @IntoSet`, sin tocar pantallas. Ver `integrations/viewer3d/README.md`.
- **Nutrición y clases grupales**: en el MVP solo botón "Próximamente" (`feature/nutrition/stub`, `feature/groups/stub`), controlado por `FeatureFlags` (`core/featureflags`). Todos los flags apagados.
- **Pagos**: la app nunca guarda datos de tarjeta (tokens del proveedor). Servicios online → Google Play Billing (o alternativo donde Google lo permita); clases presenciales → Stripe / Mercado Pago / PayPal. Cobro del trainer en el MVP: PayPal. Pendiente de validación legal.
- **Privacidad**: peso, altura y edad del alumno son privados (solo su trainer, con consentimiento). Nunca en clasificaciones.
- **Puntuación**: reglas intercambiables (`ScoringRule`). La oficial la calcula el backend; `feature/scoring/impl` es la implementación de referencia.

## Reglas de arquitectura (obligatorias)

1. Cada funcionalidad = `feature/<x>/api` (interfaces y modelos) + `feature/<x>/impl` (o `stub`).
2. Un módulo **solo** depende del `:api` de otro. Solo `:app` depende de `:impl`/`:stub`. Lo comprueba `build.gradle.kts` raíz: si se rompe, el build falla a propósito.
3. Los `api` sin UI son **Kotlin puro** (`gestor.jvm.library`). Los que exponen `@Composable` son `gestor.android.library.compose` (`muscles`, `media`, `ads`).
4. Los módulos se comunican con eventos (`core/events`: `DomainEvent`, `EventBus`), no llamándose entre sí.
5. Todo proveedor externo detrás de una interfaz.
6. Configuración de Gradle compartida en `build-logic/convention` (plugins `gestor.jvm.library`, `gestor.android.library`, `gestor.android.library.compose`, `gestor.android.feature`, `gestor.android.application`). No dupliques configuración en los módulos.

Paquete base: `com.caracolas.gestortrainer`. Ejemplo: `feature/activities/impl` → `com.caracolas.gestortrainer.feature.activities.impl`.

## Versiones

Gradle 8.14.3 (wrapper) · AGP 8.11.1 · Kotlin 2.1.21 · KSP 2.1.21-2.0.1 · Hilt 2.56.2 · Compose BOM 2025.06.00 · compileSdk/targetSdk 36 · minSdk 26 · JDK 17. Catálogo en `gradle/libs.versions.toml`.
Estas versiones no son las últimas: **no actualizar** hasta que el proyecto compile tal cual. Después se actualiza todo junto.

## ESTADO ACTUAL Y PRIMERA TAREA

Fase 0 (esqueleto) generada, **todavía no ha compilado nunca en una máquina con Android SDK**.

Historial:
1. Primera sincronización en Android Studio: falló en `:build-logic:convention:compileKotlin`. Los plugins de convención usaban `override fun apply(target: Project) = with(target) { ... }` y el tipo de retorno no era `Unit`. **Ya corregido** (cuerpo con bloque `{ with(target) { ... } }`).
2. Segunda sincronización: `build-logic` compila bien, pero se quedó **más de 11 minutos en "Configure projects…"** sin descargas. El PC estaba muy cargado (emulador abierto, CPU ~87 %). No se sabe si terminó.

Lo que sí está verificado: todo el código Kotlin puro (core/model, events, featureflags, auth, todos los `api` JVM, reglas de puntuación, validador de actividades) compila con kotlinc 2.1.21 y pasa comprobaciones equivalentes a los tests.

**Primera tarea al empezar:**
1. Desde la raíz del proyecto ejecuta: `.\gradlew.bat help --stacktrace` (en PowerShell) o `./gradlew help`. Si se cuelga en configuración, usa `--info` o `--scan` para ver dónde, y revisa:
   - el bloque `subprojects { afterEvaluate { ... } }` del `build.gradle.kts` raíz (comprobación de dependencias) — es el primer sospechoso; se puede reescribir con `configurations.configureEach { dependencies.whenObjectAdded { ... } }` o moverlo a un plugin de convención;
   - memoria del daemon (`org.gradle.jvmargs=-Xmx4g` en `gradle.properties`).
2. Luego `.\gradlew.bat assembleDebug` y `.\gradlew.bat test`. Corrige errores de compilación de los módulos Android/Compose/Hilt (nunca se han compilado).
3. Cuando compile: ejecutar la app en el emulador. Debe mostrar la elección de rol (trainer / alumno) y la barra inferior de cada rol, con botones "Próximamente".
4. Explícale a Jonathan qué corregiste, en pocas líneas.

## Siguiente: Fase 1 (MVP núcleo)

Orden sugerido (detalle en ARQUITECTURA.md §10 y en los diseños):
1. `core/network` (Retrofit + kotlinx.serialization) y `core/database` (Room) — esqueleto.
2. Onboarding + perfiles (trainer y alumno, datos personales; peso/altura privados con consentimiento).
3. ADD New Activity (formulario del diseño `TrainerNewActivity.dc.html`): tipo física (nutricional "Próximamente"), nombre, descripción, tiempo promedio oculto al alumno, músculos con la silueta (`MuscleMapRenderer`), subida de video (`MediaUploader`).
4. ADD New User: vinculación por ID y solicitudes (`LinkService`).
5. Calendario (arrastrar actividades a días) y plan semanal del alumno.
6. Notas del trainer.
7. Reproductor de video real con Media3 dentro de `VideoMediaViewer`.

Backend (AWS CDK en TypeScript, Lambda, API Gateway, Cognito, DynamoDB, EventBridge, S3 + CloudFront): servicios y API en ARQUITECTURA.md §7. Mientras no exista, usar implementaciones falsas en memoria detrás de las mismas interfaces.

## Forma de trabajar con Jonathan

- Escríbele en español, directo y breve. Explica lo que cambias y por qué en pocas líneas.
- Antes de cambios grandes de alcance (nuevas funcionalidades, cambiar decisiones de arriba), pregúntale.
- Añade tests para la lógica de dominio (reglas, validaciones) en cada funcionalidad.
- No subas nada a git ni hagas push sin que lo pida.
- `../GestorTrainer-fase0.tar.gz` es una copia de seguridad de la Fase 0 (se puede borrar).
