# x-wal — Cross-Workflow Abstraction Layer

**Technologieunabhaengiger Abstraktionslayer fuer Workflow-Engines.**

x-wal stellt eine einheitliche API fuer verschiedene BPMN-Workflow-Engines bereit (Camunda 7, Flowable, Zeebe u.a.) und ermoeglicht engineuebergreifende Migrationen, Analysen und Generierungen — ohne Vendor-Lock-in.

## Architektur

**Hexagonale Architektur (Ports & Adapters)** in Kotlin mit Micronaut 4.x.

```
x-wal/
├── hexagon/                          # Framework-frei (pure Kotlin)
│   ├── core/                         # Domain: Modelle, Validierung, Services
│   ├── ports/                        # Port-Interfaces (Input + Output)
│   └── application/                  # Use Cases
├── adapters/
│   ├── driving/                      # Primaere Adapter
│   │   ├── web/                      # REST (18 Endpoints) + gRPC
│   │   └── cli/                      # Migration-Tool (Picocli)
│   └── driven/                       # Sekundaere Adapter
│       ├── persistence/              # PostgreSQL (Micronaut Data JDBC)
│       ├── engine/                   # Camunda 7 + Flowable (REST)
│       ├── identity/                 # Keycloak (OAuth2/JWT)
│       └── observability/            # OpenTelemetry
└── app/                              # Composition Root (@Factory Wiring)
```

**Kernprinzip:** Das Hexagon (`core`, `ports`, `application`) hat **null Framework-Dependencies** — verifiziert durch automatisierte Architektur-Tests.

## Tech-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Kotlin 2.3.20 |
| Framework | Micronaut 4.9.4 |
| Build | Gradle (KTS) mit Version Catalog |
| Datenbank | PostgreSQL 16 + Flyway |
| Security | Keycloak 23 (OAuth2/JWT) |
| Observability | OpenTelemetry + Prometheus + Grafana |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| Tests | JUnit 5, MockK, Testcontainers |
| Workflow Engines | Camunda 7 (REST), Flowable 7 (REST) |

## Schnellstart

### Voraussetzungen

- JDK 21+
- Docker (fuer Testcontainers und Dev-Umgebung)

### Build

```bash
./gradlew build
```

### Dev-Umgebung starten

```bash
# Infrastruktur (PostgreSQL, Keycloak, Camunda, OTel, Prometheus, Grafana)
docker compose -f docker-compose.dev.yml up -d

# Anwendung
./gradlew :app:run
```

Die API ist unter `http://localhost:8080/api/v1/` erreichbar.

### Tests

```bash
# Alle Tests
./gradlew test

# Nur Architektur-Tests
./gradlew :app:test --tests "com.xwal.ArchitectureTest"

# Nur Persistence Integration Tests
./gradlew :adapters:driven:persistence:test
```

### CLI Migration-Tool

```bash
# IWM JSON → Camunda 7 BPMN
./gradlew :adapters:driving:cli:run --args="workflow.json -t camunda7 -o output.bpmn"

# BPMN XML → IWM JSON
./gradlew :adapters:driving:cli:run --args="process.bpmn -t flowable -o output.json"
```

## REST API

| Methode | Pfad | Beschreibung |
|---|---|---|
| `POST` | `/api/v1/workflows` | Workflow erstellen (IWM validiert) |
| `GET` | `/api/v1/workflows` | Workflows auflisten |
| `GET` | `/api/v1/workflows/{id}` | Workflow Details |
| `DELETE` | `/api/v1/workflows/{id}` | Workflow loeschen |
| `POST` | `/api/v1/workflows/{id}/start` | Instanz starten |
| `POST` | `/api/v1/workflows/instances/{id}/suspend` | Instanz pausieren |
| `POST` | `/api/v1/workflows/instances/{id}/resume` | Instanz fortsetzen |
| `POST` | `/api/v1/workflows/instances/{id}/cancel` | Instanz abbrechen |
| `GET` | `/api/v1/instances/{id}` | Instanz-Status |
| `GET` | `/api/v1/instances/{id}/variables` | Instanz-Variablen |
| `GET` | `/api/v1/tasks` | Tasks abfragen (cross-engine) |
| `GET` | `/api/v1/tasks/{id}` | Task Details |
| `POST` | `/api/v1/tasks/{id}/complete` | Task abschliessen |
| `POST` | `/api/v1/tasks/{id}/assign` | Task zuweisen |
| `POST` | `/api/v1/tasks/{id}/unassign` | Zuweisung entfernen |
| `POST` | `/api/v1/adapters` | Engine-Adapter registrieren |
| `DELETE` | `/api/v1/adapters/{id}` | Adapter deregistrieren |
| `POST` | `/api/v1/adapters/health-check` | Health-Check aller Adapter |

Authentifizierung via Keycloak JWT. Scopes: `workflow.read`, `workflow.write`, `workflow.admin`.

## Modulstruktur

| Modul | Abhaengigkeit | Beschreibung |
|---|---|---|
| `hexagon:core` | keine | Domain-Modelle, IWM-Validator, Exceptions |
| `hexagon:ports` | core | 19 Input Ports + 8 Output Ports |
| `hexagon:application` | ports, core | 19 Use Case Implementierungen |
| `adapters:driving:web` | ports | REST Controller, DTOs, gRPC Endpoints |
| `adapters:driving:cli` | ports, engine | Standalone Migration-Tool |
| `adapters:driven:persistence` | ports, core | PostgreSQL Repositories, Flyway |
| `adapters:driven:engine` | ports, core | Camunda 7 + Flowable REST Adapter |
| `adapters:driven:identity` | ports, core | Keycloak JWT + Roles Mapping |
| `adapters:driven:observability` | — | OpenTelemetry Tracer/Meter Beans |
| `app` | alle | Composition Root, @Factory Wiring |

## IWM (Intermediate Workflow Model)

Kanonisches JSON-Datenmodell fuer engineuebergreifende Workflow-Definitionen. Schema: `hexagon/core/src/main/resources/schema/iwm.schema.json` (v0.2).

Unterstuetzte Task-Typen: `activity`, `decision`, `parallel`, `map`, `subworkflow`, `event`, `timer`, `userTask`.

## Lizenz

MIT
