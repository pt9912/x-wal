# MVP-Plan v2.0.0

**Stand:** 10. April 2026
**Architektur:** Hexagonal (Kotlin/Micronaut 4.9.4)

### API-Umsetzungsstand

- REST: Kern-API vollständig dokumentiert und implementiert (18 Kern-Endpunkte).
- gRPC: Protobuf ist vorhanden und Endpoint-Implementierung in `adapters/driving/web` ist abgeschlossen.
- OpenAPI/Swagger: Spezifikation und UI sind per OpenAPI-Konfiguration verfügbar.
- Security: aktive Scopes sind `workflow.read`, `workflow.write`, `workflow.admin`.

---

## Status-Uebersicht

| Ziel | Status | Details |
|---|---|---|
| **Hexagonal Architektur** | Done | 10 Module, Domain framework-frei, 13 Architektur-Tests |
| **Domain-Kern** | Done | 17 Modelle, 8 Exceptions, 4 Services, IWM Validator |
| **Use Cases** | Done | 19 Use Cases (Workflow, Task, Adapter, Sync) |
| **REST API** | Done (Kern-API) | 18 Kern-Endpunkte, DTOs, Exception Handler, Version Filter |
| **gRPC API** | Done | workflow.proto kopiert, Endpoint-Implementierung in `adapters/driving/web` |
| **Persistenz** | Done | PostgreSQL 16, Flyway (3 Migrationen), 3 Repositories |
| **Engine-Adapter** | Done | Camunda 7 + Flowable (REST), IWM-BPMN Transformation |
| **Security** | Done | Keycloak OAuth2/JWT, Rollen-Mapping |
| **Observability** | Done | Tracer/Meter Beans bereit, Use-Case-Decorators vollständig über alle Use Cases verdrahtet |
| **Migration CLI** | Done | Picocli, IWM<->BPMN bidirektional |
| **DevOps** | Done | Dockerfile, docker-compose, DevContainer, 4 CI Workflows |
| **Tests** | Done | 77 Tests (Domain, Use Case, Persistence, Engine, Architektur) |

---

## Muss-Anforderungen (Lastenheft M-01 bis M-12)

| ID | Anforderung | Status | Implementierung |
|---|---|---|---|
| **M-01** | Einheitliches Workflow-API | Done | 18 REST Endpoints, WorkflowController, TaskController, InstanceController, AdapterController |
| **M-02** | Engine-Adapter Camunda, Flowable | Done | Camunda7Adapter, FlowableAdapter (REST-basiert), IWM-BPMN Transformer |
| **M-03** | Automatische Engine-Auswahl | Done | EngineRoutingLogic (Prioritaet, Health, Target-Engine-Hint) |
| **M-04** | Persistenz-Abstraktion | Done | PostgreSQL, Flyway, 3 Entities, 3 Repository-Adapter (hexagonal) |
| **M-05** | REST-API (OpenAPI 3) + gRPC | Done | REST komplett, OpenAPI/Swagger konfiguriert, gRPC Endpoints (WorkflowServiceEndpoint, TaskServiceEndpoint) implementiert |
| **M-06** | OAuth2/OpenID Connect (Keycloak) | Done | SecurityConfiguration, KeycloakRolesMapper, 4 Scopes |
| **M-07** | OpenTelemetry Tracing/Logging | Done | ObservabilityConfiguration (Tracer/Meter), JSON Logs, Use-Case-Decorators für alle 19 Use Cases aktiv |
| **M-08** | Docker-Compose + DevContainer | Done | docker-compose.dev.yml, .devcontainer/, Dockerfile |
| **M-09** | Test-Coverage 90%/80% | Teilweise | 77 Tests, Architektur-Tests, Coverage-Messung offen (JaCoCo) |
| **M-10** | Linux/Container-Portabilitaet | Done | Multi-Stage Dockerfile, Alpine JRE, Health Check |
| **M-11** | Engine-uebergreifende Transformation | Done | CLI Tool (IWM<->BPMN), BpmnToIwmTransformer, IwmToBpmnTransformer |
| **M-12** | IWM v0.2 Schema-Validierung | Done | IwmValidator, ValidationResult, Schema in hexagon/core |

---

## Soll-Anforderungen (S-01 bis S-07)

| ID | Anforderung | Status | Geplant |
|---|---|---|---|
| S-01 | Temporal Parser | Offen | Post-MVP |
| S-02 | Argo Workflows Parser | Offen | Post-MVP |
| S-03 | Zeebe/Camunda 8 Parser | Offen | Naechster Sprint |
| S-04 | AWS Step Functions Parser | Offen | Post-MVP |
| S-05 | Prefect 2.x Parser | Offen | Post-MVP |
| S-06 | KI-gestuetzter Generik-Parser | Offen | Post-MVP |
| S-07 | IWM-SDK (Parser/Generator) | Offen | Post-MVP |

---

## Metriken

| Metrik | Wert |
|---|---|
| Kotlin Source Files | ~125 |
| Lines of Code (Kotlin main) | ~4.750 |
| Tests | 77 |
| Test-Dateien | 14 |
| Gradle Module | 10 |
| REST Endpoints | 18 |
| Domain-Modelle | 17 (5 Value Objects, 6 Enums, 6 Data Classes) |
| Input Ports (Use Cases) | 19 |
| Output Ports | 8 |
| Flyway Migrationen | 3 |
| CI Workflows | 4 |
| Commits | 16 |

---

## Modulstruktur

```
x-wal/
hexagon/
  core/          17 Modelle, IWM Validator, Exceptions, Domain-Services
  ports/         19 Input Ports + 8 Output Ports
  application/   19 Use Case Implementierungen + AdapterResolutionService
adapters/
  driving/web/   4 Controller (18 Endpoints), DTOs, Mapper, ExceptionHandler
  driving/cli/   MigrationToolCommand (Picocli)
  driven/persistence/  3 Entities, 3 Repositories, TransactionPort, Flyway
  driven/engine/       Camunda7 + Flowable Adapter, Factory, Cache, Resilience
  driven/identity/     Keycloak JWT Validator, RolesMapper
  driven/observability/ OTel Tracer/Meter Configuration
app/             Application.kt, 4 Factory-Klassen, Scheduler, application.yml
```

---

## Offene Punkte (naechste Sprints)

### Prioritaet 1 — Funktionale Luecken

- [x] gRPC Endpoint-Implementierung (WorkflowServiceEndpoint, TaskServiceEndpoint)
- [x] Use-Case Observability-Decorators (Tracing/Metrics pro Use Case)
- [ ] JaCoCo Coverage Reports + Ziel 80%+ verifizieren
- [ ] Connection Pooling fuer Engine REST Clients (HttpClientFactory)
- [ ] Erweiterte Adapter-Endpunkte (GET /adapters, GET /adapters/enabled, GET /adapters/healthy) sind noch offen

### Prioritaet 2 — Engine-Erweiterung

- [ ] Zeebe/Camunda 8 Adapter (gRPC-basiert, S-03)
- [x] Testcontainers E2E Tests (Camunda7 + Flowable Urlaubsantrag)

### Prioritaet 3 — Betrieb

- [ ] Helm Chart fuer Kubernetes Deployment
- [ ] Grafana Dashboards (vorkonfiguriert)
- [ ] Rate Limiting / Request Throttling

### Prioritaet 4 — Post-MVP

- [ ] Parser fuer Temporal, Argo, AWS Step Functions (S-01, S-02, S-04)
- [ ] IWM-SDK Release (S-07)
- [ ] KI-gestuetzter Generik-Parser (S-06)
- [ ] Client-SDKs (Java/Kotlin, Python, Go, TypeScript) (K-07)
- [ ] BPMN-Erweiterungen DMN/CMMN (K-01)
- [ ] kotlinx-serialization Migration (Jackson ersetzen)

---

## Abnahmekriterien v2.0.0

- [x] Hexagonal Architektur: hexagon/ hat null Framework-Dependencies
- [x] Alle 19 Use Cases implementiert und via @Factory verdrahtet
- [x] 18 Kern-REST-Endpunkte funktional
- [x] Camunda 7 + Flowable Adapter funktional
- [x] IWM Schema v0.2 Validierung
- [x] Keycloak OAuth2/JWT Security
- [x] PostgreSQL Persistenz mit Flyway
- [x] CLI Migration-Tool (IWM<->BPMN)
- [x] Docker Build + docker-compose Dev-Umgebung
- [x] CI/CD Workflows (Build, Test, Docker, Docs)
- [x] 77 Tests gruen
- [x] gRPC Endpoints implementiert
- [x] Use-Case-Level Tracing/Metrics (durchgängig)
- [x] E2E Urlaubsantrag-Test: Camunda7 + Flowable
- [ ] Test-Coverage >= 80%

---

## Technologie-Stack

| Komponente | Version |
|---|---|
| Kotlin | 2.3.20 |
| Micronaut | 4.9.4 |
| Gradle | 8.5 (KTS) |
| JDK | 21 (Temurin) |
| PostgreSQL | 16 |
| Keycloak | 23.0 |
| Camunda 7 | 7.24.0 |
| Flowable | 7.0.0 |
| Resilience4j | 2.1.0 |
| OpenTelemetry | 1.32.0 |
| Testcontainers | 2.0.4 |
| KSP | 2.3.6 |
| Picocli | 4.7.5 |
