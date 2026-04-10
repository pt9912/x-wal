# x-wal Betriebshandbuch

## Docker Image

### Image beziehen

```bash
# Aktuellstes Image von GHCR
docker pull ghcr.io/pt9912/x-wal:main

# Spezifische Version
docker pull ghcr.io/pt9912/x-wal:v2.0.0
```

### Lokal bauen

```bash
docker build -t x-wal:latest .
```

### Starten

```bash
docker run -d \
  --name x-wal \
  -p 8080:8080 \
  -p 50051:50051 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=xwal \
  -e DB_USER=xwal \
  -e DB_PASSWORD=secret \
  -e KEYCLOAK_JWKS_URL=http://keycloak:8080/realms/xwal/protocol/openid-connect/certs \
  -e KEYCLOAK_ISSUER=http://keycloak:8080/realms/xwal \
  x-wal:latest
```

### Ports

| Port | Protokoll | Beschreibung |
|---|---|---|
| 8080 | HTTP | REST API + Swagger UI + Health |
| 50051 | gRPC | gRPC API (Workflow + Task Service) |

### Health Check

```bash
curl http://localhost:8080/health
```

Das Image hat einen eingebauten Health Check (alle 30s, Start-Periode 60s).

---

## Umgebungsvariablen

| Variable | Default | Beschreibung |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL Host |
| `DB_PORT` | `5432` | PostgreSQL Port |
| `DB_NAME` | `xwal` | Datenbank-Name |
| `DB_USER` | `xwal` | Datenbank-Benutzer |
| `DB_PASSWORD` | `xwal` | Datenbank-Passwort |
| `KEYCLOAK_JWKS_URL` | `http://localhost:8083/realms/xwal/protocol/openid-connect/certs` | Keycloak JWKS Endpoint |
| `KEYCLOAK_ISSUER` | `http://localhost:8083/realms/xwal` | Keycloak Issuer URL |
| `KEYCLOAK_CLIENT_ID` | `xwal-api` | OAuth2 Client ID |
| `KEYCLOAK_CLIENT_SECRET` | (leer) | OAuth2 Client Secret |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | OpenTelemetry Collector (gRPC) |
| `ENVIRONMENT` | `development` | Deployment-Umgebung |

---

## Docker Compose (Entwicklung)

Startet die komplette Entwicklungsumgebung:

```bash
docker compose -f docker-compose.dev.yml up -d
```

### Services

| Service | Port | Beschreibung |
|---|---|---|
| postgres | 5432 | PostgreSQL 16 |
| keycloak | 8083 | Keycloak 23 (Admin: admin/admin) |
| camunda7 | 8081 | Camunda BPM 7.24.0 |
| otel-collector | 4317/4318 | OpenTelemetry Collector |
| prometheus | 9090 | Metriken |
| grafana | 3000 | Dashboards (Admin: admin/admin) |

### Anwendung starten (gegen Dev-Infrastruktur)

```bash
# Option 1: Gradle
./gradlew :app:run

# Option 2: Docker (nach Build)
docker run -d \
  --name x-wal \
  --network x-wal_default \
  -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=xwal_dev_password \
  -e KEYCLOAK_JWKS_URL=http://keycloak:8080/realms/xwal/protocol/openid-connect/certs \
  -e KEYCLOAK_ISSUER=http://keycloak:8080/realms/xwal \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317 \
  x-wal:latest
```

---

## Keycloak Setup

Der Dev-Compose importiert automatisch den Realm `xwal` aus `keycloak/realm-xwal.json`.

### Rollen

| Rolle | Scopes | Beschreibung |
|---|---|---|
| `admin` | workflow.admin, workflow.write, workflow.read | Voller Zugriff |
| `workflow-designer` | workflow.write, workflow.read | Workflows erstellen/aendern |
| `workflow-executor` | workflow.write, workflow.read | Workflows ausfuehren |
| `viewer` | workflow.read | Nur lesen |

### Test-Benutzer (Dev)

| Benutzer | Passwort | Rolle |
|---|---|---|
| `admin` | `admin123` | admin |
| `designer` | `designer123` | workflow-designer |

### Token holen (Dev)

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8083/realms/xwal/protocol/openid-connect/token \
  -d "client_id=xwal-cli" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" | jq -r '.access_token')

# API aufrufen
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/workflows
```

---

## Erster Workflow (Beispiel)

### 1. Workflow erstellen

```bash
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "urlaubsantrag",
    "version": "1.0.0",
    "description": "Urlaubsantrag Workflow",
    "iwmDefinition": "{\"workflow\":{\"name\":\"urlaubsantrag\",\"version\":\"1.0.0\"},\"tasks\":[{\"id\":\"start\",\"type\":\"activity\",\"operation\":\"startProcess\",\"label\":\"Start\"},{\"id\":\"approve\",\"type\":\"userTask\",\"label\":\"Genehmigen\",\"user\":{\"assignee\":\"manager\"}},{\"id\":\"end\",\"type\":\"activity\",\"operation\":\"endProcess\",\"label\":\"Ende\"}],\"edges\":[{\"from\":\"start\",\"to\":\"approve\"},{\"from\":\"approve\",\"to\":\"end\"}]}"
  }'
```

### 2. Instanz starten

```bash
curl -X POST http://localhost:8080/api/v1/workflows/{workflowId}/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "URL-2026-001",
    "variables": {"employee": "alice", "days": 5},
    "startedBy": "alice"
  }'
```

### 3. Tasks abfragen

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/tasks?assignee=manager"
```

### 4. Task abschliessen

```bash
curl -X POST http://localhost:8080/api/v1/tasks/{taskId}/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"variables": {"approved": true}}'
```

---

## CLI Migration-Tool

Standalone-Tool fuer Offline-Migration von Workflow-Definitionen.

```bash
# IWM JSON → Camunda 7 BPMN
java -jar migration-tool.jar workflow.json -t camunda7 -o camunda.bpmn

# IWM JSON → Flowable BPMN
java -jar migration-tool.jar workflow.json -t flowable -o flowable.bpmn

# BPMN XML → IWM JSON
java -jar migration-tool.jar process.bpmn -t camunda7 -o workflow.json

# Hilfe
java -jar migration-tool.jar --help
```

Oder via Gradle:

```bash
./gradlew :adapters:driving:cli:run --args="workflow.json -t camunda7 -o output.bpmn"
```

---

## Monitoring

### Prometheus Metriken

Verfuegbar unter `http://localhost:8080/metrics` (Micrometer/Prometheus Format).

### OpenTelemetry Traces

Traces werden an den OTel Collector gesendet (OTLP gRPC auf Port 4317) und sind in Grafana/Tempo sichtbar.

### Strukturierte Logs

JSON-formatierte Logs auf stdout mit Trace-Korrelation (`traceId`, `spanId`):

```json
{"@timestamp":"2026-04-10T12:00:00.000Z","message":"Started workflow instance","traceId":"abc123","spanId":"def456"}
```

---

## Produktion

### Empfohlene Konfiguration

```yaml
# Kubernetes Deployment (Auszug)
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: xwal-db
        key: password
  - name: KEYCLOAK_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: xwal-keycloak
        key: client-secret
  - name: ENVIRONMENT
    value: production

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"

livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 60

readinessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30
```

### Horizontale Skalierung

x-wal ist stateless und kann horizontal skaliert werden. Beachte:
- Die Instance-State-Sync laeuft auf jeder Instanz — bei mehreren Replicas kann es zu doppelter Verarbeitung kommen (DistributedLock geplant)
- Der Adapter-Cache (InMemory) ist pro Instanz — kein shared State zwischen Replicas
