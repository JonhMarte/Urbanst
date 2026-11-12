# Funcionalidades adicionales — guía de implementación

Para Claude Code. Fuente: `../Datos adicionales .docx` (Jonathan, 12 nov 2026). Diseños: fila **«Adicionales (Fase 1)»** del canvas https://claude.ai/artifact/S8bAHprcfii3tt6PVckJRT y copias en `../Diseños/`.

**Requisito previo:** la Fase 0 debe compilar (`assembleDebug` y `test` en verde). Si no, termina eso primero (ver `CLAUDE.md`).

Respeta las reglas de `CLAUDE.md`: `api` + `impl`, dependencias solo entre `api`, comunicación por eventos y proveedores detrás de interfaces. Escribe tests para toda la lógica de dominio. Mientras no exista backend, usa implementaciones falsas en memoria detrás de las mismas interfaces.

---

## 0. Resumen de lo que entra

| # | Funcionalidad | Pantallas del diseño | Módulo |
|---|---|---|---|
| A | Cuestionario del alumno (objetivo, zonas, hábitos, salud, correo). Editable por el alumno y visible para su trainer | `ClientQuestionnaire1`, `ClientQuestionnaire2`, `TrainerClientProfile` | **nuevo** `feature/assessment` |
| B | Mensajería de texto trainer ↔ alumno | `Chat` (+ accesos en `TrainerClientProfile`, `ClientHomeV2`, `ClientPayment`) | **nuevo** `feature/messaging` |
| C | Inicio del alumno con carrusel de categorías populares → «Encuentra tu trainer» filtrado | `ClientHomeV2` | `feature/discovery` (ampliar) |
| D | Pagos: el trainer crea una solicitud de pago (diario / mensual / semestral / anual, monto, ID del alumno). El alumno la ve en su perfil y paga | `TrainerPayments`, `ClientPayment` | `feature/payments` (ampliar) |

---

## A. Cuestionario del alumno — `feature/assessment`

### Cuándo aparece
- **Onboarding del alumno**: después de los datos personales, 2 pasos (`ClientQuestionnaire1` → `ClientQuestionnaire2`). Se puede saltar con «Completar después».
- **Perfil del alumno** → fila «Mis objetivos y hábitos»: abre las mismas 2 pantallas en modo edición.
- **Ficha del alumno (trainer)** → `TrainerClientProfile`: solo lectura.

### Modelo (`feature/assessment/api`, Kotlin puro)

```kotlin
enum class FitnessGoal { STRENGTH, FLEXIBILITY, TONING, WEIGHT_LOSS, HYPERTROPHY, ENDURANCE }
// Fuerza, Flexibilidad, Tonificación, Bajar de peso, Hipertrofia (masa muscular), Resistencia

enum class BodyArea { SHOULDERS, CHEST, ARMS, CORE, BACK, GLUTES, LEGS, CALVES }
// Hombros, Pecho, Brazos, Abdomen, Espalda, Glúteos, Piernas, Gemelos

enum class Obstacle { TIREDNESS, LACK_OF_MOTIVATION, TOO_MUCH_WORK, LITTLE_TIME }
// Cansancio, Falta de motivación, Mucho trabajo, Poco tiempo

enum class DailyTime(val minutes: Int) { MIN_15(15), MIN_30(30), HOUR_1(60), HOURS_2(120) }

data class ClientAssessment(
    val clientId: ClientId,
    val goals: Set<FitnessGoal>,          // varias opciones
    val bodyAreas: Set<BodyArea>,
    val activityFrequency: Int,           // 0..10
    val obstacles: Set<Obstacle>,         // solo si activityFrequency < 4; si no, vacío
    val dailyTime: DailyTime?,
    val daysPerWeek: Int?,                // 1..6
    val psychomotorIssue: Boolean,
    val psychomotorDetails: String?,      // obligatorio si psychomotorIssue
    val email: String,
    val updatedAt: Instant,
)

interface AssessmentRepository {
    fun observe(clientId: ClientId): Flow<ClientAssessment?>
    suspend fun save(assessment: ClientAssessment): Result<Unit>
}

interface AssessmentValidator {
    fun validate(a: ClientAssessment): List<AssessmentError>
}
```

### Reglas (en `AssessmentValidator`, con tests)
- `activityFrequency` en 0..10. Si es **< 4**, se muestran los obstáculos. Si es ≥ 4, `obstacles` se vacía al guardar.
- `daysPerWeek` en 1..6.
- Si `psychomotorIssue = true`, `psychomotorDetails` es obligatorio (máx. 500 caracteres).
- `email` con formato válido. **No dupliques la fuente de verdad**: el correo es el de la cuenta (Cognito). Si el alumno lo cambia aquí, se actualiza en la cuenta, con verificación.
- Al guardar se publica el evento `AssessmentUpdated(clientId)` en `core/events`. Notifications avisa al trainer vinculado.

### Selector del cuerpo
- Cuerpo y botones sincronizados: tocar una zona del cuerpo o su botón activa y desactiva lo mismo.
- Amplía `feature/muscles/api` con una variante interactiva, sin romper la actual:
  ```kotlin
  interface MuscleMapRenderer {
      @Composable fun Render(selected: Set<MuscleId>, modifier: Modifier)
      @Composable fun RenderSelectable(selected: Set<MuscleId>, onToggle: (MuscleId) -> Unit, modifier: Modifier)
  }
  ```
- Mapeo `BodyArea → MuscleId`: `SHOULDERS→shoulders, CHEST→chest, ARMS→arms, CORE→core, BACK→back, GLUTES→glutes, LEGS→quads, CALVES→calves`. Añade `back` (Espalda) al `DefaultMuscleCatalog`. La silueta es frontal: «Espalda» solo se elige con su botón. Una vista trasera queda como mejora futura.
- Accesibilidad: los botones son la vía principal; el cuerpo es un atajo táctil.

### Privacidad (obligatorio)
- **«Problema psicomotriz» es un dato de salud.** Según el RGPD, es una categoría especial. Requisitos:
  - consentimiento explícito y separado antes de guardarlo (casilla, no marcada por defecto);
  - visible solo para el alumno y su trainer vinculado;
  - nunca en clasificaciones, perfiles públicos ni analítica;
  - cifrado en reposo en el backend;
  - el alumno puede borrarlo.
- Peso y altura siguen con las mismas reglas que ya teníamos.

### Pantalla del trainer (`TrainerClientProfile`)
Lee `AssessmentRepository.observe(clientId)` y `ProfileRepository`. Muestra: objetivos, zonas (con `MuscleMapRenderer.Render`), frecuencia, tiempo diario, días/semana, obstáculos, datos privados y correo. Botones **Mensaje** (abre B) y **Calendario** (pantalla existente). Se abre desde la lista de alumnos (`ClientsScreen`) al tocar un alumno.

---

## B. Mensajería — `feature/messaging`

```kotlin
data class Conversation(val id: ConversationId, val trainerId: TrainerId, val clientId: ClientId, val lastMessage: Message?, val unread: Int)
data class Message(val id: MessageId, val conversationId: ConversationId, val senderId: UserId, val text: String, val sentAt: Instant, val readAt: Instant?)

interface MessagingService {
    fun observeConversations(userId: UserId): Flow<List<Conversation>>
    fun observeMessages(conversationId: ConversationId, page: PageRequest): Flow<List<Message>>
    suspend fun send(conversationId: ConversationId, text: String): Result<MessageId>
    suspend fun markRead(conversationId: ConversationId, upTo: MessageId): Result<Unit>
    suspend fun conversationWith(trainerId: TrainerId, clientId: ClientId): Result<ConversationId>
}
```

- Añade `ConversationId` y `MessageId` a `core/model`.
- **Solo entre un trainer y un alumno vinculados.** La conversación se crea al aceptar el vínculo, escuchando el evento `ClientLinked`. Si el vínculo se rompe, la conversación queda en solo lectura.
- MVP: solo texto, máx. 2000 caracteres y sin adjuntos. Estados: enviando / enviado / leído. Optimista en UI, con reintento si falla.
- Eventos: `MessageSent`. Notifications envía un push al destinatario si la app no está en primer plano.
- Backend previsto: API Gateway WebSocket (o AppSync con subscriptions) + DynamoDB (tabla Messages, clave `conversationId` + `sentAt`). En el MVP local: implementación en memoria con un `MutableStateFlow`.
- Accesos: botón **Mensaje** en `TrainerClientProfile`; icono de mensajes con contador en el Inicio del alumno (`ClientHomeV2`); fila «Mensajes con mi trainer» en el perfil del alumno.
- Pendiente para más adelante: denunciar o bloquear, y moderación.

---

## C. Inicio del alumno con carrusel — ampliar `feature/discovery`

```kotlin
data class PopularCategory(val discipline: String, val imageUrl: String?, val trainerCount: Int)

interface DiscoveryService {
    // existentes…
    fun popularCategories(limit: Int = 6): Flow<List<PopularCategory>>
}
```

- `ClientHomeV2` **sustituye** a `ClientHome` como Inicio del alumno. Las clasificaciones completas quedan en «Ver clasificaciones», que abre `LeaderboardScreen`.
- Carrusel arriba: se desliza de derecha a izquierda (`HorizontalPager` de Compose), con flechas y puntos indicadores, y opcionalmente avance automático cada 5 s que se pausa al tocar. Cada tarjeta: imagen de la categoría (CDN; mientras tanto, color de marca), nombre, nº de trainers y «Ver trainers».
- Tocar una tarjeta abre «Encuentra tu trainer» (`DiscoveryScreen`) **ya filtrado** por esa disciplina. Cambia la firma a `DiscoveryScreen(initialDiscipline: String? = null, …)`. La navegación la conecta `:app`.
- «Populares» = disciplinas con más alumnos activos en los últimos 30 días (lo calcula el backend). En local, una lista fija.
- Debajo: tarjeta «Mi puesto / Puntos / Racha» y top 3 de trainers.

---

## D. Pagos por solicitud del trainer — ampliar `feature/payments`

### Flujo
1. **Trainer → Perfil → «Habilitación de pagos»** (`TrainerPayments`):
   - arriba, la sección **«Método de cobro»**:
     - sin cuenta, botón **«Vincular cuenta de cobro»**, que abre la pantalla **`PayoutLink`**:
       - lista de PayPal, Cuenta bancaria (Stripe Connect) y Mercado Pago; la opción elegida se despliega con «Necesitas / Recibes / Comisiones» (la comisión de la plataforma es un dato configurable, aún sin definir);
       - selector de país de la cuenta;
       - aviso: se abre la página segura del proveedor y GestorTrainer no ve contraseñas ni datos bancarios;
       - botón «Continuar con {proveedor}», que llama a `PayoutProvider.linkAccount` y abre la URL del proveedor en un **Custom Tab** (no un WebView). No diseñamos ni imitamos la página del proveedor;
     - al volver por deep link (`gestortrainer://payout/return?provider=…`) se muestra **`PayoutLinkStatus`**, que se actualiza con el webhook del proveedor:
       - `VERIFYING`: «Verificando tu cuenta», con 3 pasos (proveedor elegido ✓, inicio de sesión ✓, verificación en curso). Botones «Volver a Habilitación de pagos» y «Elegir otro método»;
       - `ACTIVE`: «Cuenta vinculada», insignia Activa, correo enmascarado. Botones «Ir a Habilitación de pagos» y «Cambiar de cuenta»;
       - `ACTION_REQUIRED`: «Falta información», con el requisito que pida el proveedor. Botones «Completar en {proveedor}» (vuelve a abrir su página) y «Elegir otro método»;
     - modelo: `data class PayoutAccount(val provider: GatewayId, val maskedEmail: String?, val status: PayoutStatus)` con `enum class PayoutStatus { VERIFYING, ACTIVE, ACTION_REQUIRED, DISCONNECTED }`, más `PayoutProvider.observeAccount(trainerId): Flow<PayoutAccount?>`;
     - con cuenta, muestra el método vinculado y el botón **«Cambiar»**;
     - **Enviar** está bloqueado sin método vinculado y muestra el aviso «Vincula un método de cobro antes de enviar el pago»;
     - un `PayoutProvider` por proveedor (`PayPalPayoutProvider`, `StripeConnectPayoutProvider`, `MercadoPagoPayoutProvider`), elegidos con un `PayoutProviderRegistry`. Guarda en el backend solo el ID de la cuenta del proveedor, nunca datos bancarios;
   - elige plan: **Diario (1 día) · Mensual (1 mes) · Semestral (6 meses) · Anual (12 meses)**;
   - monto editable, en EUR por ahora; la moneda depende de la región del trainer;
   - ID del alumno: se valida que sea un alumno **vinculado** y se muestra su nombre;
   - **Guardar**: guarda la tarifa como borrador o plantilla para ese alumno;
   - **Enviar**: crea la solicitud, estado `SENT`, y avisa al alumno.
2. **Alumno → Perfil**: aparece la fila **«Pago pendiente»** (`ClientPayment`). Al tocarla se abre la hoja con:
   - nombre e ID del trainer, plan, monto;
   - **Pagar**: abre el registro de tarjeta **en la pantalla del proveedor** (la app nunca ve los datos de la tarjeta);
   - **Cerrar**: cierra la hoja; la solicitud sigue pendiente.
3. El webhook del proveedor confirma el pago, la solicitud pasa a `PAID` y se crea un `Entitlement` del periodo correspondiente. Se publica `PaymentSucceeded`. La comisión de la plataforma se calcula en el backend y se registra en `LedgerEntry`.

### Modelo

```kotlin
enum class BillingPeriod(val days: Int) { DAILY(1), MONTHLY(30), SEMIANNUAL(182), ANNUAL(365) }
enum class PaymentRequestStatus { DRAFT, SENT, PAID, CANCELLED, EXPIRED }

data class PaymentRequest(
    val id: PaymentRequestId,
    val trainerId: TrainerId,
    val clientId: ClientId,
    val period: BillingPeriod,
    val amount: Money,
    val status: PaymentRequestStatus,
    val createdAt: Instant,
    val expiresAt: Instant?,
)

interface PaymentRequestService {
    suspend fun saveDraft(trainerId: TrainerId, clientPublicId: PublicId, period: BillingPeriod, amount: Money): Result<PaymentRequestId>
    suspend fun send(id: PaymentRequestId): Result<Unit>
    suspend fun cancel(id: PaymentRequestId): Result<Unit>
    fun observeForTrainer(trainerId: TrainerId): Flow<List<PaymentRequest>>
    fun observePendingForClient(clientId: ClientId): Flow<List<PaymentRequest>>
    /** Inicia el pago con el proveedor que decida PaymentRouter. */
    suspend fun pay(id: PaymentRequestId): PaymentResult
}
```

- Añade `PaymentRequestId` a `core/model`.
- Validaciones con tests: monto > 0 y con un máximo configurable; alumno vinculado; una sola solicitud `SENT` activa por alumno y trainer (al enviar una nueva, la anterior pasa a `CANCELLED`); expira a los 7 días sin pagar.
- Eventos: `PaymentRequested`, `PaymentSucceeded`, `RefundIssued`.

### ⚠️ Decisión pendiente de Jonathan: Google Play
- **Google Play Billing no admite montos libres puestos por cada trainer.** Los productos y precios se definen de antemano en Play Console, y las suscripciones de Play no tienen periodo diario.
- Para coaching online (servicio digital), la política de Google Play exige su sistema de pago, con las excepciones regionales de facturación alternativa que Google tenga vigentes. Verifícalas en la documentación oficial antes de decidir.
- Las **clases presenciales** (servicio físico) sí pueden cobrarse con Stripe, Mercado Pago o PayPal y montos libres.
- Opciones a plantearle:
  1. online con niveles de precio fijos en Play, donde el trainer elige un nivel y no un monto libre;
  2. monto libre solo para presencial;
  3. pagos externos donde Google lo permita.
- **Implementa el flujo detrás de `PaymentRouter`** y usa en local un `FakePaymentGateway` más Stripe en modo test. **No publiques en Play con cobro online hasta que Jonathan decida.** Pregúntale antes de elegir.

---

## Navegación (cambios en `:app`)

- **Alumno:** Inicio = `ClientHomeV2`. Perfil = hub con «Pago pendiente» (si existe), «Mis objetivos y hábitos», «Datos personales» y «Mensajes con mi trainer».
- **Trainer:** en Usuarios, tocar un alumno abre `TrainerClientProfile` (antes iba directo al calendario). Perfil añade la fila «Habilitación de pagos».
- **Onboarding del alumno:** datos personales → cuestionario 1/2 → 2/2 → Inicio.

## Backend (añadir a los servicios de ARQUITECTURA.md §7)

| Servicio | Nuevo |
|---|---|
| Assessments | Tabla por `clientId`; detalle psicomotriz cifrado. Publica `AssessmentUpdated` |
| Messaging | WebSocket/AppSync + DynamoDB; escucha `ClientLinked`; publica `MessageSent` |
| Discovery | `GET /discovery/categories/popular` |
| Payments | `POST /payment-requests` (draft), `POST /payment-requests/{id}/send`, `POST /payment-requests/{id}/cancel`, `GET /me/payment-requests?status=SENT`, `POST /payment-requests/{id}/pay` → URL o sesión del proveedor |

## Orden sugerido y criterios de aceptación

1. **A — Cuestionario.** Módulo, validador con tests, las 2 pantallas, la ficha del trainer y la edición desde el perfil.
   ✔ Si la frecuencia es < 4 aparecen los obstáculos. ✔ Tocar el cuerpo o un botón marca la zona en ambos. ✔ El trainer ve los datos y el alumno los edita. ✔ El dato psicomotriz pide consentimiento.
2. **C — Carrusel.** ✔ Desliza, tiene puntos y flechas. ✔ Al tocar una categoría se abre la búsqueda filtrada.
3. **B — Mensajería** (en memoria primero). ✔ Envío y recepción en el mismo dispositivo con dos usuarios simulados. ✔ Contador de no leídos. ✔ No se puede escribir a un usuario no vinculado.
4. **D — Pagos.** Solicitud, envío, pendiente en el perfil, «Pagar» con gateway falso y luego Stripe test. ✔ Guardar, Enviar, Cerrar y Pagar se comportan como en el diseño. ✔ Al pagar se activa el periodo. **Pregunta a Jonathan lo de Google Play antes del cobro real.**

Al terminar cada punto: tests en verde y un resumen breve para Jonathan con lo hecho y cualquier decisión pendiente.
