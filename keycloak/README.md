# Keycloak Security Configuration

## Sprint 2 Task 7 - M-06

Diese Konfiguration stellt OAuth2/JWT-basierte Authentifizierung und Autorisierung für die x-wal API bereit.

## Realm: xwal

### Rollen

| Rolle | Beschreibung | Scopes |
|-------|--------------|--------|
| `admin` | Vollzugriff auf API | `workflow.admin`, `workflow.write`, `workflow.read` |
| `workflow-designer` | Workflows erstellen/ändern | `workflow.write`, `workflow.read` |
| `workflow-executor` | Workflows starten/stoppen | `workflow.write`, `workflow.read` |
| `viewer` | Nur Lesezugriff | `workflow.read` |

### Test-Benutzer

| Username | Password | Rolle | Email |
|----------|----------|-------|-------|
| `admin` | `admin123` | admin | admin@xwal.local |
| `designer` | `designer123` | workflow-designer | designer@xwal.local |
| `executor` | `executor123` | workflow-executor | executor@xwal.local |
| `viewer` | `viewer123` | viewer | viewer@xwal.local |

### Clients

#### xwal-api (Backend)
- **Type**: Bearer-Only
- **Purpose**: Backend API Service
- **Authentication**: JWT Token Validation

#### xwal-cli (CLI/Testing)
- **Type**: Public Client
- **Purpose**: CLI Tool und Testing
- **Flows**: Direct Grant, Standard Flow
- **Redirect URIs**: `http://localhost:*`, `http://127.0.0.1:*`

## Verwendung

### 1. Keycloak starten

```bash
docker-compose -f docker-compose.dev.yml up -d keycloak
```

Keycloak Admin Console: http://localhost:8083
- Username: `admin`
- Password: `admin`

### 2. JWT Token holen (für Tests)

#### Mit curl (Direct Grant Flow)

```bash
# Admin Token
curl -X POST http://localhost:8083/realms/xwal/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=xwal-cli" \
  -d "username=admin" \
  -d "password=admin123"

# Designer Token
curl -X POST http://localhost:8083/realms/xwal/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=xwal-cli" \
  -d "username=designer" \
  -d "password=designer123"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "...",
  "token_type": "Bearer"
}
```

### 3. API aufrufen mit Token

```bash
export TOKEN="<access_token from above>"

# List workflows (requires workflow.read)
curl -X GET http://localhost:8080/api/v1/workflows \
  -H "Authorization: Bearer $TOKEN"

# Create workflow (requires workflow.write)
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-workflow",
    "version": "1.0",
    "iwmDefinition": {...}
  }'
```

### 4. Token validieren

```bash
curl -X POST http://localhost:8083/realms/xwal/protocol/openid-connect/token/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=$TOKEN" \
  -d "client_id=xwal-cli"
```

## Konfiguration

### application.yml

Die x-wal API ist konfiguriert um JWT Tokens von Keycloak zu validieren:

```yaml
micronaut:
  security:
    enabled: true
    token:
      jwt:
        enabled: true
        signatures:
          jwks:
            keycloak:
              url: http://localhost:8083/realms/xwal/protocol/openid-connect/certs
    oauth2:
      enabled: true
      clients:
        keycloak:
          client-id: xwal-api
          openid:
            issuer: http://localhost:8083/realms/xwal
```

### Environment Variables

Für Docker/DevContainer:

```bash
KEYCLOAK_JWKS_URL=http://keycloak:8080/realms/xwal/protocol/openid-connect/certs
KEYCLOAK_ISSUER=http://keycloak:8080/realms/xwal
KEYCLOAK_CLIENT_ID=xwal-api
KEYCLOAK_CLIENT_SECRET=  # Leer für bearer-only clients
```

## Security Flow

1. **Client Authentication**: User authentifiziert sich bei Keycloak
2. **Token Issuance**: Keycloak gibt JWT Access Token aus
3. **API Request**: Client sendet Request mit `Authorization: Bearer <token>` Header
4. **Token Validation**: x-wal API validiert Token gegen Keycloak JWKS
5. **Claims Extraction**: `KeycloakRolesMapper` extrahiert Rollen aus Token
6. **Authorization**: `@Secured` Annotations prüfen erforderliche Scopes
7. **Access Granted/Denied**: API gibt 200 OK oder 403 Forbidden zurück

## JWT Token Struktur

### Claims

- `iss`: Issuer (Keycloak Realm)
- `sub`: Subject (User ID)
- `exp`: Expiration Time
- `iat`: Issued At
- `realm_access.roles`: Realm-Rollen (admin, workflow-designer, etc.)
- `resource_access.xwal-api.roles`: Client-spezifische Rollen

### Scope Mapping

Die `KeycloakRolesMapper` Klasse mappt Keycloak Rollen zu x-wal Scopes:

```java
admin -> workflow.admin, workflow.write, workflow.read
workflow-designer -> workflow.write, workflow.read
workflow-executor -> workflow.write, workflow.read
viewer -> workflow.read
```

## Troubleshooting

### Token Validation Failed

```
Status: 401 Unauthorized
```

**Ursachen**:
- Token abgelaufen (expires_in: 300s = 5 Minuten)
- Falscher Issuer (muss `http://localhost:8083/realms/xwal` sein)
- Ungültige Signatur

**Lösung**: Neuen Token holen

### Insufficient Permissions

```
Status: 403 Forbidden
```

**Ursachen**:
- User hat nicht die erforderliche Rolle
- Scope fehlt im Token

**Lösung**: User mit passender Rolle verwenden

### Keycloak nicht erreichbar

```
Error: Connection refused to localhost:8083
```

**Lösung**:
```bash
docker-compose -f docker-compose.dev.yml up -d keycloak
docker-compose -f docker-compose.dev.yml logs -f keycloak
```

Warte bis Keycloak bereit ist: `Listening on: http://0.0.0.0:8080`

## Produktions-Hinweise (Sprint 5+)

Für Produktion:

1. **HTTPS aktivieren**: KC_HTTPS_ENABLED=true
2. **Hostname konfigurieren**: KC_HOSTNAME=keycloak.production.com
3. **Client Secret setzen**: xwal-api Client Secret konfigurieren
4. **Token Lifetime anpassen**: Kürzere Expiration für Produktion
5. **Rate Limiting**: Keycloak Rate Limiting aktivieren
6. **Audit Logging**: Admin Events und User Events loggen

## Referenzen

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/)
- [Pflichtenheft M-06](../docs/Pflichtenheft-x-wal-v1.5.0.md#m-06)
