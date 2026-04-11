# Migration: x-wal-v1 (Java/Layered) -> x-wal (Kotlin/Hexagonal)

**Stand:** 10. April 2026 (Rev. 2 — Review-Korrekturen eingearbeitet)
**Quelle:** `/Development/x-wal-v1` (Java 21, Micronaut 4.x, ~13.500 LOC, ~340 Tests)
**Ziel:** `/Development/x-wal` (Kotlin, Micronaut 4.x, Hexagonale Architektur)

### API-Umsetzungsstand

- REST: vollständig dokumentiert und implementiert (18 Endpoints).
- gRPC: Protobuf ist vorhanden; Endpoint-Implementierung in `adapters/driving/web` ist noch offen.
- OpenAPI/Swagger: Spezifikation und UI sind noch offen und als Folgeaufgabe geplant.
- Security: aktive Scopes sind `workflow.read`, `workflow.write`, `workflow.admin`.

---

## Kontext

x-wal-v1 verwendet eine klassische Schichtenarchitektur (Controller -> Service -> Repository) in Java. Die Geschaeftslogik (WorkflowService, TaskService, AdapterRegistryService, EngineRoutingService) liegt im `api`-Modul zusammen mit Entities, Repositories, DTOs, Security und Observability. Entities sind gleichzeitig Domain-Modelle und Persistenz-Objekte. Services haengen direkt von Micronaut-Data-Repositories ab.

Ziel ist eine saubere hexagonale Architektur in Kotlin, bei der das gesamte Hexagon (`core`, `ports`, `application`) **framework-frei** bleibt und alle Infrastruktur ueber Ports & Adapters angebunden wird.

---

## Design-Entscheidungen (aus Review)

| # | Entscheidung | Wahl |
|---|---|---|
| 1 | hexagon/application framework-frei? | **Ja (puristisch)** — kein `@Singleton`, kein Micronaut im Hexagon. Bean-Registration via `@Factory` im `app`-Modul |
| 2 | Adapter-Cache State | **Output Port** — `AdapterInstanceCachePort` in `hexagon/ports`, InMemory-Impl in `driven/engine`, spaeter Redis austauschbar |
| 3 | Observability Cross-Cutting | **Decorator-Pattern** — Use-Case-Decorators im `app`-Modul fuer Tracing/Metrics. Use Cases bleiben 100% sauber |
| 4 | Transaktionsmanagement | **Output Port** — `TransactionPort` in `hexagon/ports`, Impl in `driven/persistence` |
| 5 | Identity separates Modul? | **Ja** — eigenes Modul behalten (spaeter austauschbar, z.B. Keycloak -> Auth0) |
| 6 | Version Catalog | **Ja** — `gradle/libs.versions.toml` fuer zentrale Versionspflege |
| 7 | Flyway-Pfad | Explizit in `application.yml` konfigurieren (`classpath:db/migration`) |

---

## Modulstruktur

```
x-wal/
├── hexagon/                                  # Innerhalb des Hexagons (KEIN Framework)
│   ├── core/                                 # Domain: Modelle, Validierung, Typsystem
│   ├── ports/                                # Port-Interfaces (Input + Output)
│   └── application/                          # Use Cases (Orchestrierung, pure Kotlin)
│
├── adapters/                                 # Ausserhalb des Hexagons (Framework erlaubt)
│   ├── driving/                              # Primaere Adapter (rufen Use Cases auf)
│   │   ├── web/                              # REST Controllers + gRPC Endpoints
│   │   └── cli/                              # Migration-Tool CLI (Picocli)
│   └── driven/                               # Sekundaere Adapter (implementieren Output Ports)
│       ├── persistence/                      # PostgreSQL, Micronaut Data JDBC, Flyway
│       ├── engine/                           # Camunda7, Flowable Adapter + REST Clients
│       ├── identity/                         # Keycloak OAuth2/JWT
│       └── observability/                    # OpenTelemetry, Metrics, Tracing
│
├── app/                                      # Composition Root
│   ├── src/main/kotlin/com/xwal/
│   │   ├── Application.kt                   # Micronaut.run()
│   │   ├── factory/                          # @Factory: Bean-Registration aller Use Cases
│   │   ├── decorator/                        # Observability-Decorators fuer Use Cases
│   │   ├── config/                           # @ConfigurationProperties
│   │   └── scheduler/                        # @Scheduled Jobs
│   └── src/main/resources/
│       ├── application.yml
│       └── logback.xml
│
├── gradle/
│   └── libs.versions.toml                    # Zentrale Versionsverwaltung
├── docs/
├── keycloak/
├── observability/
├── docker-compose.yml
├── docker-compose.dev.yml
├── Dockerfile
├── build.gradle.kts
└── settings.gradle.kts
```

### Abhaengigkeitsrichtung (strikt durchgesetzt via Gradle)

```
hexagon/core        <-- haengt von NICHTS ab (reines Kotlin + Jackson + JSON Schema Validator)
hexagon/ports       --> hexagon/core
hexagon/application --> hexagon/ports, hexagon/core

adapters/driving/web  --> hexagon/ports, hexagon/core  (NICHT hexagon/application!)
adapters/driving/cli  --> hexagon/ports, hexagon/core, adapters/driven/engine
adapters/driven/*     --> hexagon/ports, hexagon/core

app (Composition Root) --> ALLE Module
```

**Regeln:**
- Kein Hexagon-Modul (`core`, `ports`, `application`) darf ein Adapter-Modul oder Framework importieren
- Kein Adapter-Modul darf ein anderes Adapter-Modul importieren (Ausnahme: `cli` → `engine`)
- Controller/Endpoints injizieren Port-Interfaces (aus `ports`), nicht Implementierungen (aus `application`)
- Das Wiring (Port-Interface → Implementierung) passiert ausschliesslich im `app`-Modul via `@Factory`

---

## Modul-Details

### hexagon/core — Domain-Kern

Reines Kotlin. **Keine** Micronaut-Annotations, **keine** Framework-Dependencies.

```
com.xwal.domain/
  model/
    Workflow.kt                    data class (aus IwmWorkflow Entity)
    WorkflowId.kt                  value class (UUID Wrapper)
    WorkflowStatus.kt              enum: DRAFT, ACTIVE, DEPRECATED, ARCHIVED
    WorkflowInstance.kt            data class (aus IwmInstance Entity)
    InstanceId.kt                  value class
    InstanceStatus.kt              enum: RUNNING, SUSPENDED, COMPLETED, FAILED, CANCELLED, TERMINATED
    EngineAdapterConfig.kt         data class (aus EngineAdapter Entity)
    EngineAdapterId.kt             value class
    EngineType.kt                  enum: CAMUNDA7, CAMUNDA8, FLOWABLE, ACTIVITI, TEMPORAL, CONDUCTOR
    HealthStatus.kt                enum: HEALTHY, UNHEALTHY, UNKNOWN
    Task.kt                        data class (aus adapter TaskInfo)
    TaskId.kt                      value class (composite: engineType:actualId)
    TaskFilter.kt                  data class
    IwmDefinition.kt               value class (String-Wrapper fuer JSONB)
    AdapterCapabilities.kt         data class (19 Feature-Flags)
    HistoricInstanceInfo.kt        data class
    Incident.kt                    data class
    EngineInstanceStatus.kt        data class (Runtime-Status von Engine)
  validation/
    IwmValidator.kt                portiert aus core/IwmValidator.java
    ValidationResult.kt            portiert aus core/ValidationResult.java
  exception/
    DomainException.kt             sealed class Basis
    WorkflowNotFoundException.kt
    InstanceNotFoundException.kt
    WorkflowValidationException.kt
    AdapterNotFoundException.kt
    AdapterUnavailableException.kt
    InvalidStateTransitionException.kt
  service/
    IwmValidationService.kt        Validierung + Schema-Versionierung
    EngineRoutingLogic.kt           Adapter-Auswahl-Logik (pure Funktion)
    TaskAggregationLogic.kt         Cross-Engine Task-Aggregation
    HistoryResolutionLogic.kt       deleteReason -> Status Mapping
```

**Beispiel Domain-Modell:**

```kotlin
data class Workflow(
    val id: WorkflowId,
    val name: String,
    val version: String,
    val description: String?,
    val iwmDefinition: IwmDefinition,
    val status: WorkflowStatus,
    val createdAt: Instant,
    val createdBy: String?,
    val updatedAt: Instant,
    val updatedBy: String?
)

@JvmInline value class WorkflowId(val value: UUID)
@JvmInline value class IwmDefinition(val json: String)
```

### hexagon/ports — Port-Interfaces

Definiert Input Ports (Use-Case-Interfaces) und Output Ports. Haengt nur von `hexagon/core` ab. **Kein Framework.**

```
com.xwal.domain.port/
  input/                              Use-Case Interfaces
    CreateWorkflowUseCase.kt
    GetWorkflowUseCase.kt
    ListWorkflowsUseCase.kt
    StartWorkflowUseCase.kt
    SuspendInstanceUseCase.kt
    ResumeInstanceUseCase.kt
    CancelInstanceUseCase.kt
    GetInstanceUseCase.kt
    GetInstanceVariablesUseCase.kt
    QueryTasksUseCase.kt
    CompleteTaskUseCase.kt
    AssignTaskUseCase.kt
    GetTaskUseCase.kt
    RegisterAdapterUseCase.kt
    DeregisterAdapterUseCase.kt
    HealthCheckAdapterUseCase.kt
    SyncInstanceStateUseCase.kt
  output/                             Sekundaere Ports
    WorkflowRepository.kt             domain-owned Interface
    InstanceRepository.kt
    EngineAdapterConfigRepository.kt
    WorkflowEnginePort.kt             aus WorkflowEngineAdapter
    EngineAdapterFactoryPort.kt        aus EngineAdapterFactory
    AdapterInstanceCachePort.kt        Adapter-Instanz-Cache (InMemory oder Redis)
    TransactionPort.kt                 Programmatische Transaktionssteuerung
    DistributedLockPort.kt
```

**Beispiel Input Port:**

```kotlin
interface CreateWorkflowUseCase {
    fun execute(command: CreateWorkflowCommand): Workflow

    data class CreateWorkflowCommand(
        val name: String,
        val version: String,
        val description: String?,
        val iwmDefinition: String,
        val createdBy: String?
    )
}
```

**Beispiel Output Ports:**

```kotlin
interface WorkflowRepository {
    fun save(workflow: Workflow): Workflow
    fun findById(id: WorkflowId): Workflow?
    fun findByNameAndVersion(name: String, version: String): Workflow?
    fun findAllActive(): List<Workflow>
    fun findAll(): List<Workflow>
    fun existsByNameAndVersion(name: String, version: String): Boolean
    fun countByStatus(status: WorkflowStatus): Long
}

// Adapter-Instanz-Cache — MVP: InMemory, spaeter: Redis
interface AdapterInstanceCachePort {
    fun get(id: EngineAdapterId): WorkflowEnginePort?
    fun put(id: EngineAdapterId, adapter: WorkflowEnginePort)
    fun evict(id: EngineAdapterId)
    fun getAllHealthy(): List<Pair<EngineAdapterId, WorkflowEnginePort>>
}

// Programmatische Transaktionen (statt @Transactional Annotation)
interface TransactionPort {
    fun <T> executeInTransaction(block: () -> T): T
}
```

### hexagon/application — Use Case Implementierungen

Orchestriert Domain-Services und Output-Ports. **Reines Kotlin, KEIN Framework.**
Bean-Registration erfolgt im `app`-Modul via `@Factory`.

```
com.xwal.application.usecase/
  workflow/
    CreateWorkflowUseCaseImpl.kt
    GetWorkflowUseCaseImpl.kt
    ListWorkflowsUseCaseImpl.kt
    StartWorkflowUseCaseImpl.kt
    SuspendInstanceUseCaseImpl.kt
    ResumeInstanceUseCaseImpl.kt
    CancelInstanceUseCaseImpl.kt
    GetInstanceUseCaseImpl.kt
    GetInstanceVariablesUseCaseImpl.kt
  task/
    QueryTasksUseCaseImpl.kt
    CompleteTaskUseCaseImpl.kt
    AssignTaskUseCaseImpl.kt
    GetTaskUseCaseImpl.kt
  adapter/
    RegisterAdapterUseCaseImpl.kt
    DeregisterAdapterUseCaseImpl.kt
    HealthCheckAdapterUseCaseImpl.kt
  sync/
    SyncInstanceStateUseCaseImpl.kt
```

**Beispiel Use Case (kein @Singleton, kein Framework):**

```kotlin
class CreateWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository,
    private val transactionPort: TransactionPort,
    private val iwmValidationService: IwmValidationService
) : CreateWorkflowUseCase {

    override fun execute(command: CreateWorkflowCommand): Workflow {
        iwmValidationService.validateOrThrow(command.iwmDefinition)

        return transactionPort.executeInTransaction {
            if (workflowRepository.existsByNameAndVersion(command.name, command.version)) {
                throw WorkflowValidationException("Duplikat: ${command.name} v${command.version}")
            }

            val workflow = Workflow(
                id = WorkflowId(UUID.randomUUID()),
                name = command.name,
                version = command.version,
                description = command.description,
                iwmDefinition = IwmDefinition(command.iwmDefinition),
                status = WorkflowStatus.ACTIVE,
                createdAt = Instant.now(),
                createdBy = command.createdBy,
                updatedAt = Instant.now(),
                updatedBy = null
            )

            workflowRepository.save(workflow)
        }
    }
}
```

### app — Composition Root (Bean-Wiring + Decorators)

Das `app`-Modul ist der einzige Ort mit Framework-Annotations fuer das Hexagon.
Hier werden Use Cases als Beans registriert und mit Observability-Decorators gewrapped.

```
com.xwal/
  Application.kt                         Micronaut.run()
  factory/
    WorkflowUseCaseFactory.kt            @Factory: registriert Workflow Use Cases
    TaskUseCaseFactory.kt                @Factory: registriert Task Use Cases
    AdapterUseCaseFactory.kt             @Factory: registriert Adapter Use Cases
  decorator/
    TracedUseCaseDecorator.kt            Generischer Tracing-Decorator
    MeteredUseCaseDecorator.kt           Generischer Metrics-Decorator
  config/
    InstanceSyncConfig.kt                @ConfigurationProperties
    XwalGrpcConfiguration.kt
  scheduler/
    InstanceStateSyncScheduler.kt        @Scheduled
```

**Beispiel Factory (Bean-Registration + Decorator-Wiring):**

```kotlin
@Factory
class WorkflowUseCaseFactory {

    @Singleton
    fun createWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort,
        iwmValidationService: IwmValidationService,
        tracer: Tracer,
        meter: Meter
    ): CreateWorkflowUseCase {
        val impl = CreateWorkflowUseCaseImpl(
            workflowRepository, transactionPort, iwmValidationService
        )
        return TracedCreateWorkflowUseCase(impl, tracer, meter)
    }

    // ... weitere Use Cases analog
}
```

**Beispiel Observability-Decorator:**

```kotlin
// app/decorator/ — Decorator fuer CreateWorkflowUseCase
class TracedCreateWorkflowUseCase(
    private val delegate: CreateWorkflowUseCase,
    private val tracer: Tracer,
    private val meter: Meter
) : CreateWorkflowUseCase {

    private val counter = meter.counterBuilder("workflow.creates.total").build()

    override fun execute(command: CreateWorkflowCommand): Workflow {
        val span = tracer.spanBuilder("workflow.create").startSpan()
        return try {
            span.makeCurrent().use {
                span.setAttribute("workflow.name", command.name)
                counter.add(1)
                delegate.execute(command)
            }
        } catch (e: Exception) {
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }
}
```

### adapters/driving/web — REST + gRPC

Micronaut Controller und gRPC Endpoints. Injiziert **Use-Case Port-Interfaces** (aus `hexagon/ports`).

```
com.xwal.adapter.web/
  rest/
    controller/
      WorkflowController.kt          7 Endpoints -> CreateWorkflow/Get/List/StartUseCase
      InstanceController.kt           4 Endpoints -> Suspend/Resume/Cancel/GetInstance
      TaskController.kt               5 Endpoints -> QueryTasks/Complete/Assign/Get
      AdapterController.kt            9 Endpoints -> Register/Deregister/HealthCheck
    dto/
      request/                        CreateWorkflowRequest, StartInstanceRequest, ...
      response/                       WorkflowResponse, InstanceResponse, TaskResponse, ...
    mapper/
      WorkflowDtoMapper.kt           Domain <-> DTO
      InstanceDtoMapper.kt
      TaskDtoMapper.kt
    filter/
      ApiVersionResponseFilter.kt
    exception/
      GlobalExceptionHandler.kt       Domain-Exceptions -> HTTP Status Codes
  grpc/
    WorkflowServiceEndpoint.kt       9 RPCs
    TaskServiceEndpoint.kt           2 RPCs
    mapper/
      GrpcWorkflowMapper.kt
      GrpcTaskMapper.kt
  proto/                              workflow.proto, task.proto
```

**Controller injiziert Ports, nicht Implementierungen:**

```kotlin
@Controller("/api/v1/workflows")
@Secured(SecurityRule.IS_AUTHENTICATED)
class WorkflowController(
    private val createWorkflow: CreateWorkflowUseCase,   // Port aus hexagon/ports
    private val getWorkflow: GetWorkflowUseCase,
) {
    @Post
    @Secured("workflow.write")
    fun create(@Body request: CreateWorkflowRequest): HttpResponse<WorkflowResponse> {
        val workflow = createWorkflow.execute(request.toCommand())
        return HttpResponse.created(WorkflowDtoMapper.toResponse(workflow))
    }
}
```

### adapters/driven/persistence — PostgreSQL

Implementiert domain Repository-Interfaces + TransactionPort via Micronaut Data JDBC.

```
com.xwal.adapter.persistence/
  entity/
    WorkflowEntity.kt                @MappedEntity("iwm_workflows")
    InstanceEntity.kt                 @MappedEntity("iwm_instances")
    EngineAdapterEntity.kt            @MappedEntity("engine_adapters")
  repository/
    MnWorkflowRepository.kt          @JdbcRepository, CrudRepository<WorkflowEntity, UUID>
    MnInstanceRepository.kt
    MnAdapterRepository.kt
  adapter/
    WorkflowRepositoryAdapter.kt     implements domain WorkflowRepository
    InstanceRepositoryAdapter.kt
    AdapterConfigRepositoryAdapter.kt
    MicronautTransactionAdapter.kt   implements TransactionPort
  mapper/
    WorkflowEntityMapper.kt          Entity <-> Domain Model
    InstanceEntityMapper.kt
    AdapterEntityMapper.kt
  lock/
    PostgresDistributedLockAdapter.kt implements DistributedLockPort
```

**TransactionPort Implementierung:**

```kotlin
@Singleton
class MicronautTransactionAdapter(
    private val transactionManager: SynchronousTransactionManager<Connection>
) : TransactionPort {

    override fun <T> executeInTransaction(block: () -> T): T {
        return transactionManager.executeWrite { _ -> block() }
    }
}
```

**Flyway-Migrationen:** `src/main/resources/db/migration/V1__initial_schema.sql` (1:1 aus v1).
Pfad wird explizit in `app/src/main/resources/application.yml` konfiguriert:

```yaml
flyway:
  datasources:
    default:
      enabled: true
      locations: classpath:db/migration
```

### adapters/driven/engine — Workflow-Engine Adapter

Implementiert `WorkflowEnginePort` + `EngineAdapterFactoryPort` + `AdapterInstanceCachePort`.

```
com.xwal.adapter.engine/
  camunda7/
    Camunda7Adapter.kt               implements WorkflowEnginePort
    client/Camunda7RestClient.kt      ~700 LOC REST Client
    transformer/IwmToBpmnTransformer.kt
  flowable/
    FlowableAdapter.kt               implements WorkflowEnginePort
    client/FlowableRestClient.kt      ~680 LOC REST Client
    transformer/IwmToBpmnTransformer.kt
  factory/
    EngineAdapterFactoryImpl.kt       implements EngineAdapterFactoryPort
  cache/
    InMemoryAdapterCache.kt           implements AdapterInstanceCachePort (ConcurrentHashMap)
  config/
    HttpClientFactory.kt
    HttpClientPoolConfig.kt
  resilience/
    AdapterResilientDecorator.kt      Decorator: Circuit Breaker, Retry
    Resilience4jConfig.kt
    DeadLetterQueue.kt
  transformer/
    BpmnToIwmTransformer.kt           BPMN XML -> IWM JSON
```

**AdapterInstanceCachePort MVP-Implementierung:**

```kotlin
@Singleton
class InMemoryAdapterCache : AdapterInstanceCachePort {
    private val cache = ConcurrentHashMap<EngineAdapterId, WorkflowEnginePort>()

    override fun get(id: EngineAdapterId) = cache[id]
    override fun put(id: EngineAdapterId, adapter: WorkflowEnginePort) { cache[id] = adapter }
    override fun evict(id: EngineAdapterId) { cache.remove(id) }
    override fun getAllHealthy() = cache.entries.map { it.key to it.value }
}

// Spaeter austauschbar durch:
// @Singleton @Replaces(InMemoryAdapterCache::class)
// class RedisAdapterConfigCache(...) : AdapterInstanceCachePort { ... }
```

**Hinweis:** Bei Redis werden Config-Daten (URL, EngineType, Health) gecached, nicht lebende HTTP-Clients. Instanz-Erzeugung bleibt lokal.

### adapters/driven/identity — Keycloak

```
com.xwal.adapter.identity/
  SecurityConfiguration.kt            @Factory, JWT Claims Validator
  KeycloakRolesMapper.kt              Role Extraction + Scope Mapping
  GrpcJwtAuthInterceptor.kt
```

### adapters/driven/observability — OpenTelemetry

```
com.xwal.adapter.observability/
  config/
    ObservabilityConfiguration.kt      @Factory: Tracer + Meter Beans
  metrics/
    AdapterMetrics.kt                  Engine-Adapter-spezifische Metriken
    AdapterMetricsDecorator.kt         Decorator: WorkflowEnginePort -> WorkflowEnginePort + Metriken
```

**Hinweis:** Use-Case-Level Tracing/Metrics werden NICHT hier implementiert, sondern via Decorators im `app`-Modul (siehe oben). Dieses Modul stellt nur die `Tracer`/`Meter` Beans bereit und die Adapter-Level Metriken.

---

## Gradle-Konfiguration

### settings.gradle.kts

```kotlin
rootProject.name = "x-wal"

include(
    "hexagon:core",
    "hexagon:ports",
    "hexagon:application",
    "adapters:driving:web",
    "adapters:driving:cli",
    "adapters:driven:persistence",
    "adapters:driven:engine",
    "adapters:driven:identity",
    "adapters:driven:observability",
    "app"
)
```

### gradle/libs.versions.toml — Zentrale Versionsverwaltung

```toml
[versions]
kotlin = "2.3.20"
ksp = "2.3.20-1.0.31"
micronaut = "4.9.4"
micronaut-plugin = "4.6.1"
jackson = "2.16.0"
grpc = "1.62.2"
protobuf = "3.25.3"
otel = "1.32.0"
resilience4j = "2.1.0"
camunda7 = "7.24.0"
zeebe = "8.3.4"
flowable = "7.0.0"
postgresql = "42.7.1"
flyway = "9.22.3"
logback = "1.4.14"
junit = "5.10.1"
mockk = "1.13.9"
testcontainers = "1.19.3"
assertj = "3.24.2"
json-schema-validator = "2.0.0"
picocli = "4.7.5"
jgrapht = "1.5.2"

[libraries]
# Jackson
jackson-databind = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
jackson-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin", version.ref = "jackson" }
jackson-jsr310 = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310", version.ref = "jackson" }

# JSON Schema
json-schema-validator = { module = "com.networknt:json-schema-validator", version.ref = "json-schema-validator" }

# gRPC
grpc-protobuf = { module = "io.grpc:grpc-protobuf", version.ref = "grpc" }
grpc-stub = { module = "io.grpc:grpc-stub", version.ref = "grpc" }
grpc-netty = { module = "io.grpc:grpc-netty", version.ref = "grpc" }
grpc-testing = { module = "io.grpc:grpc-testing", version.ref = "grpc" }

# OpenTelemetry
otel-api = { module = "io.opentelemetry:opentelemetry-api", version.ref = "otel" }
otel-sdk = { module = "io.opentelemetry:opentelemetry-sdk", version.ref = "otel" }
otel-exporter-otlp = { module = "io.opentelemetry:opentelemetry-exporter-otlp", version.ref = "otel" }

# Resilience
resilience4j-circuitbreaker = { module = "io.github.resilience4j:resilience4j-circuitbreaker", version.ref = "resilience4j" }
resilience4j-retry = { module = "io.github.resilience4j:resilience4j-retry", version.ref = "resilience4j" }

# Workflow Engines
camunda7-rest = { module = "org.camunda.bpm:camunda-engine-rest-core", version.ref = "camunda7" }
zeebe-client = { module = "io.camunda:zeebe-client-java", version.ref = "zeebe" }
flowable-engine = { module = "org.flowable:flowable-engine", version.ref = "flowable" }

# Testing
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
junit-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
testcontainers = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" }
testcontainers-junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
testcontainers-postgres = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
assertj = { module = "org.assertj:assertj-core", version.ref = "assertj" }
jgrapht = { module = "org.jgrapht:jgrapht-core", version.ref = "jgrapht" }
logback = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
micronaut-application = { id = "io.micronaut.application", version.ref = "micronaut-plugin" }
micronaut-library = { id = "io.micronaut.library", version.ref = "micronaut-plugin" }
micronaut-test-resources = { id = "io.micronaut.test-resources", version.ref = "micronaut-plugin" }
protobuf = { id = "com.google.protobuf", version = "0.9.4" }
```

### build.gradle.kts (Root)

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.micronaut.application) apply false
    alias(libs.plugins.micronaut.library) apply false
    alias(libs.plugins.micronaut.test.resources) apply false
}

group = "com.xwal"
version = "2.0.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories { mavenCentral() }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        "implementation"("org.slf4j:slf4j-api:2.0.9")
        "testImplementation"(kotlin("test"))
        "testImplementation"(libs.junit.api)
        "testRuntimeOnly"(libs.junit.engine)
        "testImplementation"(libs.mockk)
    }

    tasks.test { useJUnitPlatform() }
}
```

### hexagon/core/build.gradle.kts — KEIN Micronaut

```kotlin
dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.json.schema.validator)
}
```

### hexagon/ports/build.gradle.kts — KEIN Micronaut

```kotlin
dependencies {
    api(project(":hexagon:core"))
}
```

### hexagon/application/build.gradle.kts — KEIN Micronaut

```kotlin
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
}
```

### adapters/driving/web/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.security:micronaut-security-annotations")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.security:micronaut-security-oauth2")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.grpc:micronaut-grpc-server-runtime")
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation("io.swagger.core.v3:swagger-annotations")
}
```

### adapters/driven/persistence/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.postgresql:postgresql")
}
```

### adapters/driven/engine/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    implementation("io.micronaut:micronaut-http-client")
    implementation(libs.jackson.kotlin)
    implementation(libs.camunda7.rest)
    implementation(libs.zeebe.client)
    implementation(libs.flowable.engine)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.otel.api)
    implementation(libs.jgrapht)
}
```

### adapters/driven/identity/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.security:micronaut-security-annotations")
    implementation("io.micronaut.security:micronaut-security-oauth2")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.grpc:micronaut-grpc-server-runtime")
}
```

### adapters/driven/observability/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    implementation("io.micronaut.tracing:micronaut-tracing-opentelemetry")
    implementation(libs.otel.api)
    implementation(libs.otel.sdk)
    implementation(libs.otel.exporter.otlp)
    implementation("io.micrometer:micrometer-core")
}
```

### app/build.gradle.kts — Composition Root

```kotlin
plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.test.resources)
    jacoco
}
dependencies {
    implementation(project(":hexagon:core"))
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:application"))
    implementation(project(":adapters:driving:web"))
    implementation(project(":adapters:driven:persistence"))
    implementation(project(":adapters:driven:engine"))
    implementation(project(":adapters:driven:identity"))
    implementation(project(":adapters:driven:observability"))
    ksp("io.micronaut:micronaut-inject-kotlin")
    runtimeOnly(libs.logback)
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")
}
application {
    mainClass.set("com.xwal.ApplicationKt")
}
micronaut {
    version("4.9.4")
    runtime("netty")
    testRuntime("junit5")
}
```

---

## Migrations-Phasen

### Phase 1: Projekt-Skelett + Domain-Kern (2-3 Tage)

**1.1 Gradle Multi-Module Skelett**
- Root `build.gradle.kts` + `settings.gradle.kts` + `gradle/libs.versions.toml`
- Alle 10 Module anlegen (leere `build.gradle.kts` + `src/` Verzeichnisse)
- `gradle.properties` kopieren und anpassen
- Verifizierung: `./gradlew build` kompiliert

**1.2 Domain-Modelle portieren (hexagon/core)**
- Aus `api/entity/IwmWorkflow.java` -> `Workflow.kt` data class (ohne `@MappedEntity`)
- Aus `api/entity/IwmInstance.java` -> `WorkflowInstance.kt` data class
- Aus `api/entity/EngineAdapter.java` -> `EngineAdapterConfig.kt` data class
- Alle Enums: `WorkflowStatus`, `InstanceStatus`, `EngineType`, `HealthStatus`
- Value Objects: `WorkflowId`, `InstanceId`, `EngineAdapterId`, `IwmDefinition`, `TaskId`
- Adapter-Modelle (POJOs): `Task`, `AdapterCapabilities`, `TaskFilter`, etc.

**1.3 Port-Interfaces definieren (hexagon/ports)**
- 17 Input Ports (Use-Case-Interfaces)
- 8 Output Ports (inkl. `AdapterInstanceCachePort`, `TransactionPort`, `DistributedLockPort`)
- `WorkflowEnginePort` = Kotlin-Version von `WorkflowEngineAdapter`

**1.4 Domain-Services portieren (hexagon/core)**
- `IwmValidator` + `ValidationResult` aus `core/`
- `EngineRoutingLogic`, `TaskAggregationLogic`, `HistoryResolutionLogic`
- IWM Schema JSON nach `hexagon/core/src/main/resources/schema/`

**1.5 Domain-Exceptions (hexagon/core)**
- Sealed class Hierarchie

**Verifizierung:** `./gradlew :hexagon:core:test :hexagon:ports:build`

### Phase 2: Application-Modul (1-2 Tage)

**2.1 Use-Case-Implementierungen (hexagon/application)**
- 17 Use Cases als pure Kotlin Klassen (kein `@Singleton`)
- Constructor Injection von Output Ports
- `TransactionPort` fuer Transaktionsgrenzen

**2.2 Unit Tests**
- MockK-basierte Tests fuer jeden Use Case

**Verifizierung:** `./gradlew :hexagon:application:test`

### Phase 3: Persistence-Adapter (2-3 Tage)

**3.1 Persistence Entities + Micronaut Data Repositories**
**3.2 Repository-Adapter (implements domain Ports)**
**3.3 TransactionPort Implementierung (MicronautTransactionAdapter)**
**3.4 Flyway-Migrationen (1:1 kopiert)**
**3.5 Integration Tests (Testcontainers PostgreSQL)**

**Verifizierung:** `./gradlew :adapters:driven:persistence:test`

### Phase 4: Engine-Adapter (2-3 Tage)

**4.1 Camunda7 + Flowable portieren (Java -> Kotlin)**
**4.2 Factory (implements EngineAdapterFactoryPort)**
**4.3 InMemoryAdapterCache (implements AdapterInstanceCachePort)**
**4.4 Resilience Decorators + HttpClientFactory**
**4.5 Tests (Unit + Testcontainers)**

**Verifizierung:** `./gradlew :adapters:driven:engine:test`

### Phase 5: Web-Adapter + gRPC (2-3 Tage)

**5.1 REST Controller (injizieren Port-Interfaces)**
**5.2 DTOs + Mapper**
**5.3 gRPC Endpoints + Proto-Dateien**
**5.4 Exception Handler + Filter**
**5.5 Tests**

**Verifizierung:** `./gradlew :adapters:driving:web:test`

### Phase 6: Identity + Observability + Composition Root (1-2 Tage)

**6.1 Identity-Adapter (Keycloak)**
**6.2 Observability-Adapter (OTel Beans + Adapter-Metriken)**
**6.3 Composition Root:**
- `Application.kt`
- `@Factory` Klassen: Bean-Registration aller Use Cases
- Observability-Decorators fuer Use Cases
- `application.yml` (inkl. explizitem Flyway-Pfad)
- Scheduler

**Verifizierung:** `./gradlew :app:run` — Anwendung startet

### Phase 7: Integration + Verifikation (2-3 Tage)

**7.1 Full-Stack Tests** (25 REST + 11 gRPC)
**7.2 Verhaltensaequivalenz** (OpenAPI Spec Vergleich)
**7.3 E2E Urlaubsantrag-Test** (Camunda7 + Flowable)
**7.4 Architektur-Checks:**
- `hexagon/core` hat keine Micronaut-Dependency
- `hexagon/ports` hat keine Micronaut-Dependency
- `hexagon/application` hat keine Micronaut-Dependency
- Kein Adapter importiert ein anderes Adapter-Modul

### Phase 8: CLI Migration-Tool (0.5-1 Tag)

- Picocli + Kotlin
- Haengt von `hexagon:ports` + `adapters:driven:engine` ab

---

## Technische Entscheidungen

| Entscheidung | Wahl | Begruendung |
|---|---|---|
| **Kotlin Version** | 2.3.20 | Aktuelle stabile Version, K2 Compiler |
| **Annotation Processing** | KSP (nicht kapt) | Micronaut 4.x empfiehlt KSP fuer Kotlin |
| **Mocking** | MockK (statt Mockito) | Idiomatisch Kotlin |
| **Coroutines** | Nein (MVP) | Behavioral Parity mit v1 |
| **IWM Definition** | `value class IwmDefinition(val json: String)` | Typ-Sicherheit ohne Jackson im Domain |
| **EngineType** | Ein Enum im Domain (statt 2 in v1) | Eliminiert `convertToAdapterEngineType()` |
| **Proto-Codegen** | Java (Standard) | Kotlin ruft generierten Java-Code auf |
| **Entity Mapping** | Manuelle Mapper | Kein MapStruct noetig bei 3 Entities |
| **DI im Hexagon** | Keines | `@Factory` im `app`-Modul registriert Beans |
| **Transaktionen** | `TransactionPort` | Framework-frei im Hexagon |
| **Adapter-Cache** | `AdapterInstanceCachePort` | Austauschbar (InMemory -> Redis) |
| **Observability** | Decorator-Pattern im `app`-Modul | Use Cases bleiben 100% sauber |
| **Versions** | `libs.versions.toml` | Zentrale Pflege fuer 10 Module |

---

## Risiken und Mitigationen

| Risiko | Wahrscheinlichkeit | Mitigation |
|---|---|---|
| Micronaut Data JDBC + Kotlin data class + JSONB | Mittel | Spike-Test in Phase 3 vorab |
| gRPC Protobuf + KSP Integration | Gering | Proto generiert Java, Kotlin ruft Java auf |
| `@Secured` Annotations mit KSP | Gering | `ksp("micronaut-security-annotations")` |
| Test-Coverage Regression (~340 Tests, ~80%) | Mittel | Tests parallel zu jedem Modul portieren |
| Decorator-Boilerplate (17 Use Cases) | Mittel | Generischer TracedUseCase-Wrapper |
| `@Factory` Boilerplate im app-Modul | Mittel | Gruppierung nach Domaene (3-4 Factory-Klassen) |

---

## Mapping: v1 Dateien -> v2 Module

| v1 Datei | v2 Modul | v2 Datei |
|---|---|---|
| `core/iwm/IwmValidator.java` | hexagon/core | `domain/validation/IwmValidator.kt` |
| `core/iwm/ValidationResult.java` | hexagon/core | `domain/validation/ValidationResult.kt` |
| `api/entity/IwmWorkflow.java` | hexagon/core + driven/persistence | `domain/model/Workflow.kt` + `entity/WorkflowEntity.kt` |
| `api/entity/IwmInstance.java` | hexagon/core + driven/persistence | `domain/model/WorkflowInstance.kt` + `entity/InstanceEntity.kt` |
| `api/entity/EngineAdapter.java` | hexagon/core + driven/persistence | `domain/model/EngineAdapterConfig.kt` + `entity/EngineAdapterEntity.kt` |
| `api/service/WorkflowService.java` | hexagon/application | 9 Use-Case-Implementierungen |
| `api/service/TaskService.java` | hexagon/application | 4 Use-Case-Implementierungen |
| `api/service/AdapterRegistryService.java` | hexagon/application | 3 Use-Case-Implementierungen |
| `api/service/EngineRoutingService.java` | hexagon/core + hexagon/application | `service/EngineRoutingLogic.kt` + Use Cases |
| `api/service/IwmRegistryService.java` | hexagon/core | `service/IwmValidationService.kt` |
| `api/repository/WorkflowRepository.java` | hexagon/ports + driven/persistence | `port/output/WorkflowRepository.kt` + `WorkflowRepositoryAdapter.kt` |
| `api/controller/WorkflowController.java` | driving/web | `rest/controller/WorkflowController.kt` |
| `api/grpc/WorkflowServiceEndpoint.java` | driving/web | `grpc/WorkflowServiceEndpoint.kt` |
| `adapters/WorkflowEngineAdapter.java` | hexagon/ports | `port/output/WorkflowEnginePort.kt` |
| `adapters/camunda7/Camunda7Adapter.java` | driven/engine | `camunda7/Camunda7Adapter.kt` |
| `adapters/flowable/FlowableAdapter.java` | driven/engine | `flowable/FlowableAdapter.kt` |
| `api/security/SecurityConfiguration.java` | driven/identity | `SecurityConfiguration.kt` |
| `api/observability/TracingService.java` | driven/observability | `ObservabilityConfiguration.kt` |
| — (neu) | app/factory | `WorkflowUseCaseFactory.kt` (Bean-Registration) |
| — (neu) | app/decorator | `TracedCreateWorkflowUseCase.kt` (Observability) |

---

## Geschaetzter Gesamtaufwand

| Phase | Dauer | Komplexitaet |
|---|---|---|
| Phase 1: Skelett + Domain | 2-3 Tage | Mittel |
| Phase 2: Application | 1-2 Tage | Gering-Mittel |
| Phase 3: Persistence | 2-3 Tage | Mittel |
| Phase 4: Engine Adapter | 2-3 Tage | Mittel |
| Phase 5: Web + gRPC | 2-3 Tage | Mittel |
| Phase 6: Identity + Obs + App | 1-2 Tage | Gering |
| Phase 7: Integration Tests | 2-3 Tage | Mittel-Hoch |
| Phase 8: CLI Tool | 0.5-1 Tag | Gering |
| **Gesamt** | **13-20 Tage** | |

---

## Offene Punkte (Stand: 11. April 2026)

Die folgenden 15 Dateien aus dem Migrationsplan wurden zurueckgestellt. Sie sind fuer die naechsten Sprints vorgesehen.

### Prioritaet 1 — gRPC API (M-05)

| Datei | Modul | Plan-Zeile | Beschreibung |
|---|---|---|---|
| `WorkflowServiceEndpoint.kt` | adapters/driving/web/grpc | 411 | 9 gRPC RPCs (CreateWorkflow, GetWorkflow, ListWorkflows, StartInstance, GetInstance, SuspendInstance, ResumeInstance, CancelInstance, GetInstanceVariables) |
| `TaskServiceEndpoint.kt` | adapters/driving/web/grpc | 412 | 2 gRPC RPCs (QueryTasks, CompleteTask) |
| `GrpcWorkflowMapper.kt` | adapters/driving/web/grpc/mapper | 414 | Proto-Messages <-> Domain-Modelle |
| `GrpcTaskMapper.kt` | adapters/driving/web/grpc/mapper | 415 | Proto-Messages <-> Domain-Modelle |
| `GrpcJwtAuthInterceptor.kt` | adapters/driven/identity | 544 | JWT-Authentifizierung fuer gRPC Calls |
| `XwalGrpcConfiguration.kt` | app/config | 328 | gRPC Server Konfiguration |

**Voraussetzung:** Protobuf-Gradle-Plugin muss in `adapters/driving/web/build.gradle.kts` konfiguriert werden (Code-Generierung aus `workflow.proto`).

### Prioritaet 2 — Observability Decorators (M-07)

| Datei | Modul | Plan-Zeile | Beschreibung |
|---|---|---|---|
| `TracedUseCaseDecorator.kt` | app/decorator | 324 | Generischer Tracing-Decorator — wrapping aller Use Cases mit OpenTelemetry Spans |
| `MeteredUseCaseDecorator.kt` | app/decorator | 325 | Generischer Metrics-Decorator — Counter/Histogramme pro Use Case |
| `AdapterMetrics.kt` | adapters/driven/observability | 554 | Engine-Adapter-spezifische Metriken (Call-Latenz, Erfolgsrate) |
| `AdapterMetricsDecorator.kt` | adapters/driven/observability | 555 | Decorator: WorkflowEnginePort -> WorkflowEnginePort + Metriken |

**Design-Entscheidung:** Decorators werden im `app`-Modul via `@Factory` gewired. Use Cases bleiben framework-frei. Siehe Design-Entscheidung #3.

### Prioritaet 3 — Infrastruktur

| Datei | Modul | Plan-Zeile | Beschreibung |
|---|---|---|---|
| `PostgresDistributedLockAdapter.kt` | adapters/driven/persistence/lock | 461 | Implementiert `DistributedLockPort` via PostgreSQL Advisory Locks. Benoetigt fuer Multi-Replica `SyncInstanceState`. |
| `InstanceSyncConfig.kt` | app/config | 327 | `@ConfigurationProperties` fuer `xwal.instance-sync.*` (batch-size, timeout, retry). Aktuell via `@Value` Interpolation. |
| `HttpClientFactory.kt` | adapters/driven/engine/config | 508 | Connection Pooling fuer Engine REST Clients. Aktuell: `HttpClient.create()` ohne Pool. |
| `HttpClientPoolConfig.kt` | adapters/driven/engine/config | 509 | Pool-Konfiguration (max-connections, timeouts, TTL). |
| `DeadLetterQueue.kt` | adapters/driven/engine/resilience | 513 | Speichert fehlgeschlagene Operationen fuer spaetere Wiederholung. Aktuell: Fehler werden nur geloggt. |

### Zusammenfassung

| Prioritaet | Dateien | Abhaengigkeit |
|---|---|---|
| 1 — gRPC | 6 | Protobuf-Codegen konfigurieren |
| 2 — Observability | 4 | Tracer/Meter Beans vorhanden |
| 3 — Infrastruktur | 5 | Unabhaengig voneinander |
| **Gesamt** | **15** | |

---

## Verifikation

Nach Abschluss muessen folgende Kriterien erfuellt sein:

- [ ] `./gradlew build` kompiliert alle 10 Module ohne Fehler
- [ ] `./gradlew test` alle Tests gruen
- [ ] **Architektur-Checks:**
  - [ ] `hexagon/core` hat **keine** Micronaut-Dependency
  - [ ] `hexagon/ports` hat **keine** Micronaut-Dependency
  - [ ] `hexagon/application` hat **keine** Micronaut-Dependency
  - [ ] Kein Adapter-Modul importiert ein anderes Adapter-Modul
- [ ] OpenAPI Spec identisch zu v1
- [ ] Alle 25 REST Endpoints funktional identisch
- [ ] Alle 11 gRPC RPCs funktional identisch
- [ ] Flyway-Migration auf frischer DB erfolgreich
- [ ] Keycloak JWT Auth funktioniert
- [ ] Testcontainers: Camunda7 + Flowable Integration Tests gruen
- [ ] E2E Urlaubsantrag-Test: Camunda7 <-> Flowable
- [ ] Docker Build + `docker-compose up` funktioniert
