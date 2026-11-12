# GestorTrainer

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.06.00-4285F4)
![minSdk](https://img.shields.io/badge/minSdk-26-informational)
![Estado](https://img.shields.io/badge/MVP-Fase%202%20%2B%20Adicionales-success)

App Android (Kotlin + Jetpack Compose) que conecta **entrenadores** de cualquier disciplina con sus **alumnos**: el trainer crea actividades, vincula alumnos por ID y les asigna un calendario semanal; el alumno activa/completa sus actividades, sube evidencia en foto y suma puntos frente a clasificaciones globales. Un proyecto de **Caracolas Games**.

## ¿Para quién?

| | |
|---|---|
| 🏋️ **Trainer** | Crea su biblioteca de actividades, vincula alumnos por ID, les asigna un plan en el calendario, deja notas y ve su propia puntuación/reputación frente a otros trainers. |
| 🙋 **Alumno** | Ve su plan semanal, activa y finaliza cada actividad (con foto de evidencia), gana puntos por constancia y tiempo, valora a su trainer con estrellas y comentarios, y se compara en clasificaciones. |

## Estado actual del MVP

| Fase | Contenido | Estado |
|---|---|---|
| **Fase 0** | Esqueleto modular, convención de Gradle, Hilt, módulos `api`/`impl`/`stub` | ✅ Completa |
| **Fase 1** | Onboarding, perfiles, ADD New Activity, ADD New User + vinculación, calendario, plan semanal, notas del trainer, cerrar sesión | ✅ Completa |
| **Fase 2** | Foto de evidencia al completar, puntuación real, las 3 clasificaciones, estrellas y comentarios | ✅ Completa |
| **Adicionales** | Cuestionario del alumno, mensajería trainer-alumno, carrusel de categorías, pagos (solicitud del trainer), pipeline de descubrimiento "solicitud → propuesta → pago" | ✅ Completa (ver [`ADICIONALES.md`](ADICIONALES.md)) |
| **Backend AWS** | API Gateway, Lambda, Cognito, DynamoDB, EventBridge, S3+CloudFront | ⏳ Pendiente — hoy todo vive en memoria, detrás de las mismas interfaces |

Todo lo que hoy es "en memoria" (`InMemory*`, `Default*`) implementa un contrato (`:api`) pensado para sustituirse por el backend real sin tocar la UI ni las reglas de negocio.

## Arquitectura

La fuente de verdad del diseño es [`../ARQUITECTURA.md`](../ARQUITECTURA.md) (MVP v2: módulos, interfaces, backend, pagos, puntuación, privacidad, plan por fases) y los diseños de pantalla en [`../Diseños/`](../Diseños/).

Reglas obligatorias de este repo:

1. Cada funcionalidad = `feature/<x>/api` (interfaces y modelos) + `feature/<x>/impl` (o `stub`).
2. Un módulo **solo** depende del `:api` de otro. Solo `:app` depende de `:impl`/`:stub` — si se rompe, el build falla a propósito (comprobación en `build.gradle.kts` raíz).
3. Los `api` sin UI son Kotlin puro (plugin `gestor.jvm.library`); los que exponen `@Composable` usan `gestor.android.library.compose`.
4. Los módulos se comunican publicando `DomainEvent` en `core/events` (`EventBus`), no llamándose entre sí.
5. Todo proveedor externo vive detrás de una interfaz (pagos, media, red…).
6. Configuración de Gradle compartida en `build-logic/convention` — nunca duplicada en cada módulo.

```
app/                       único módulo que conoce las implementaciones (:impl/:stub)
build-logic/convention/    plugins de convención de Gradle (gestor.android.feature, etc.)
core/
  model/                   value classes e identificadores (UserId, ClientId, TrainerId…)
  events/                  DomainEvent + EventBus (pub/sub entre módulos)
  featureflags/            flags de funciones reservadas (nutrición, grupos, 3D…), todas apagadas
  designsystem/             botones, PlaceholderScreen y estilos compartidos
  auth/                    sesión (sin login real todavía)
  network/ database/       Retrofit/kotlinx.serialization y Room — esqueleto para el backend
feature/<nombre>/api/      contratos: interfaces + modelos
feature/<nombre>/impl/     implementación (Compose + Hilt), o /stub si está reservado
  onboarding   profile   activities   muscles   media
  clients      calendar  plan         tracking
  scoring      leaderboards  reviews  discovery
  assessment   messaging matching     payments
  ads          notifications
  nutrition/stub  groups/stub   ← "Próximamente"
integrations/viewer3d/     FUTURO: visor 3D/AR (Unity as a Library), punto de integración listo
```

### Punto de integración 3D/AR

El MVP no incluye VR/AR: todo pasa por el teléfono. Pero `ExerciseMediaViewer` + `MediaViewerRegistry` (`feature/media`) ya están preparados para que un futuro `integrations/viewer3d` añada un visor 3D con `@Binds @IntoSet`, sin tocar ninguna pantalla existente.

## Lo que ya funciona

- **Onboarding y perfiles**: registro de trainer/alumno, datos personales, peso/altura/edad privados (solo el trainer vinculado los ve, nunca en clasificaciones).
- **ADD New Activity**: tipo física (nutricional detrás de flag), músculos con silueta 2D (`MuscleMapRenderer`), subida de video del instructor.
- **ADD New User**: vinculación por ID público, solicitudes pendientes/aceptar/rechazar.
- **Calendario y plan semanal**: el trainer asigna actividades por día; el alumno las ve, las activa y las completa.
- **Cerrar sesión** y un atajo de "Volver a entrar" (sin backend de login todavía) para probar dos cuentas en un mismo dispositivo.
- **Foto de evidencia**: al finalizar una actividad se sube una foto y se registra la sesión (`feature:tracking`).
- **Puntuación real**: reglas intercambiables (`ScoringRule`) por tiempo dentro del margen, foto subida, día completo y racha semanal, con tope diario (`feature:scoring`); la oficial la calculará el backend con las mismas reglas.
- **Tres clasificaciones** (`feature:leaderboards`): alumnos de un trainer, alumnos globales y trainers globales (ranking bayesiano por estrellas + cumplimiento + retención).
- **Estrellas y comentarios** (`feature:reviews`): el alumno valora a su trainer vinculado; el trainer ve su resumen.
- **Notas del trainer** a cada alumno.
- **Reproductor de vídeo real** con Media3/ExoPlayer (`VideoMediaViewer`).
- **Cuestionario del alumno** (`feature:assessment`): objetivos, zonas del cuerpo sobre la silueta, frecuencia de actividad, tiempo/días disponibles, consentimiento explícito para el dato de salud psicomotriz (nunca en clasificaciones); ficha de solo lectura para el trainer vinculado.
- **Mensajería** (`feature:messaging`): chat de texto trainer-alumno, conversación automática al vincularse, contador de no leídos.
- **Carrusel de categorías** en el nuevo Inicio del alumno (`ClientHomeV2`): disciplinas populares con auto-avance, enlaza a "Encuentra tu trainer" filtrado.
- **Pagos** (`feature:payments`): el trainer vincula un método de cobro (PayPal/Stripe Connect/Mercado Pago, proveedores de prueba) y envía solicitudes de pago puntuales (diario/mensual/semestral/anual) a un alumno vinculado.
- **Pipeline de descubrimiento "solicitud → propuesta → pago"** (`feature:matching`, en paralelo al vínculo por ID, que sigue igual): perfil público del trainer, solicitud del alumno con consentimiento granular (qué comparte), bandeja de solicitudes del trainer, propuesta con precio/duración/caducidad, y pago (Stripe en modo test) que vincula al alumno al aceptar.
- Botones "Próximamente" para nutrición y clases grupales, controlados por `FeatureFlags`.

## Versiones

Gradle 8.14.3 (wrapper) · AGP 8.11.1 · Kotlin 2.1.21 · KSP 2.1.21-2.0.1 · Hilt 2.56.2 · Compose BOM 2025.06.00 · compileSdk/targetSdk 36 · minSdk 26 · JDK 17. Catálogo completo en `gradle/libs.versions.toml`.

## Cómo compilar y probar

```bash
# Linux/macOS
./gradlew assembleDebug
./gradlew test

# Windows
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

Abre el proyecto en Android Studio (**Open** → esta carpeta) y ejecuta la configuración **app** en un emulador o móvil con Android 8.0+. La app arranca con la elección de rol (trainer / alumno).

## Modelo de negocio (según ARQUITECTURA.md)

Comisión por transacción, publicidad (solo plan gratuito del alumno) y servicios directos. Pagos: Google Play Billing para servicios online, Stripe/Mercado Pago/PayPal para clases presenciales; la app nunca guarda datos de tarjeta, solo tokens del proveedor.

## Próximos pasos

- Backend AWS real (CDK en TypeScript: Lambda, API Gateway, Cognito, DynamoDB, EventBridge, S3+CloudFront) detrás de las interfaces ya existentes (`core/network` y `core/database` ya tienen el esqueleto — cliente Retrofit, mapeo de errores, cola de sincronización offline — pero ningún módulo lo usa todavía).
- Selector de archivos / cámara real para fotos y vídeos (hoy se simulan con una URL fija).
- Pagos reales: decisión pendiente sobre Google Play Billing (no admite montos libres ni periodo diario) vs. Stripe/Mercado Pago/PayPal para el cobro online; hasta entonces todo pasa por gateways de prueba.
- Nutrición y clases grupales (hoy son stubs tras `FeatureFlags`).
- Visor 3D/AR (`integrations/viewer3d`, Unity as a Library) cuando el punto de integración lo requiera.

---

Para el contexto completo de trabajo con Claude Code en este repo, ver [`CLAUDE.md`](CLAUDE.md).
