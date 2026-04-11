# Migrationsplan: Testcontainers 1.21.4 → 2.0.4

**Erstellt:** 2026-04-11  
**Status:** umgesetzt (Dokumentation angepasst)

**Betroffene Module:** `app`, `adapters:driven:persistence`, `adapters:driven:engine`

---

## 1. Zusammenfassung der Breaking Changes

Testcontainers 2.0 bringt folgende wesentliche Änderungen:

| Kategorie | 1.21.4 (aktuell) | 2.0.4 (Ziel) |
|-----------|-------------------|---------------|
| **Artifact-Namen** | `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| | `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |
| | `org.testcontainers:testcontainers` | `org.testcontainers:testcontainers` (unverändert) |
| **Package-Namen** | `org.testcontainers.containers.PostgreSQLContainer` | `org.testcontainers.postgresql.PostgreSQLContainer` |
| **API-Entfernungen** | `getContainerIpAddress()` verfügbar | entfernt → `getHost()` verwenden |

---

## 2. Bestandsaufnahme (Ist-Zustand)

### 2.1 Dependencies

**gradle/libs.versions.toml:**
```gradle
testcontainers = "2.0.4"

testcontainers-core = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" }
testcontainers-junit = { module = "org.testcontainers:testcontainers-junit-jupiter", version.ref = "testcontainers" }
testcontainers-postgres = { module = "org.testcontainers:testcontainers-postgresql", version.ref = "testcontainers" }
```

**Modulabhängigkeiten:**
```gradle
// app/build.gradle.kts
testImplementation(libs.testcontainers.core)
testImplementation(libs.testcontainers.junit)
testImplementation(libs.testcontainers.postgres)

// adapters/driven/persistence/build.gradle.kts
testImplementation(libs.testcontainers.core)
testImplementation(libs.testcontainers.junit)
testImplementation(libs.testcontainers.postgres)

// adapters/driven/engine/build.gradle.kts
testImplementation(libs.testcontainers.core)
testImplementation(libs.testcontainers.junit)
```

### 2.2 Betroffene Testdateien

**`org.testcontainers.postgresql.PostgreSQLContainer`: 0 Dateien**  
Es werden aktuell keine PostgreSQLContainer-Imports im Quellcode gefunden.

**`org.testcontainers.containers.GenericContainer`: 2 Dateien**
- `adapters/driven/engine/src/test/kotlin/com/xwal/adapter/engine/integration/Camunda7IntegrationTest.kt`
- `adapters/driven/engine/src/test/kotlin/com/xwal/adapter/engine/integration/FlowableIntegrationTest.kt`

### 2.3 Verwendete Patterns

| Pattern | Vorkommen | Änderung nötig? |
|---------|-----------|-----------------|
| `@Testcontainers` | 2 Dateien | Nein |
| `@Container` | 2 Dateien | Nein |
| `GenericContainer` | 2 Dateien | Nein (bleibt in `org.testcontainers.containers`) |
| `org.testcontainers.postgresql.PostgreSQLContainer` | 0 | Nein |
| `DockerImageName.parse()` | 0 | Nein |
| `Wait.forHttp()` | 2 Dateien | Nein |
| `getHost()` | 0 | Nein |

---

## 3. Migrationsschritte (Umgesetzt)

### Schritt 1: Zentralisierung bestätigt

Projekt nutzt bereits den zentralen Versionskatalog.  
Es wurde festgelegt, **keine hartkodierten Versionsangaben** in Modul-`build.gradle.kts` zu verwenden.

### Schritt 2: Versionskatalog auf 2.0.4 angehoben

In [gradle/libs.versions.toml](/Development/x-wal/gradle/libs.versions.toml) die folgenden Änderungen durchgeführt:

```diff
- testcontainers = "1.21.4"
+ testcontainers = "2.0.4"

- testcontainers-junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
- testcontainers-postgres = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
+ testcontainers-junit = { module = "org.testcontainers:testcontainers-junit-jupiter", version.ref = "testcontainers" }
+ testcontainers-postgres = { module = "org.testcontainers:testcontainers-postgresql", version.ref = "testcontainers" }
```

### Schritt 3: Konsistenzprüfung auf alte PostgreSQLContainer-Imports

```bash
rg -n "org\\.testcontainers\\.containers\\.PostgreSQLContainer|org\\.testcontainers\\.postgresql\\.PostgreSQLContainer"
```

Im aktuellen Zustand sind keine Treffer im Source-Verzeichnis vorhanden.

### Schritt 4: Micronaut Test Resources prüfen

Die Anwendung nutzt Micronaut **4.9.4** über `micronautVersion` im `gradle.properties` und das `micronaut-test-resources` Plugin aus dem Versionskatalog.
Vor dem Start der Testläufe prüfen:

- [ ] Micronaut Test Resources Release Notes auf Testcontainers 2.x-Kompatibilität prüfen
- [ ] Ggf. `micronaut-test-resources-testcontainers` explizit versionieren, wenn erforderlich

### Schritt 5: Build- und Testläufe ausführen

```bash
./gradlew :app:testClasses :adapters:driven:persistence:testClasses :adapters:driven:engine:testClasses
./gradlew :app:test :adapters:driven:persistence:test :adapters:driven:engine:test
./gradlew :adapters:driven:engine:test --tests '*IntegrationTest*'
```

### Schritt 6: Verifizierung

- [ ] Alte Testcontainer-Importe aufgelöst/korrekt
- [ ] Alle betroffenen Modul-Tests laufen grün
- [ ] Testcontainer-basierte Integrationstests stabil
- [ ] Docker-Umgebung bleibt kompatibel (Ryuk/Reuse-Verhalten falls konfiguriert)

---

## 4. Risikobewertung

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|---------------------|------------|------------|
| Micronaut Test Resources inkompatibel mit TC 2.x | Niedrig | Mittel | Vorab-Kompatibilität prüfen (Schritt 4) |
| `GenericContainer`-Verhalten ändert sich | Niedrig | Mittel | Nutzung unverändert in `containers`-Package |
| Docker-Version-Inkompatibilität | Niedrig | Hoch | Lokale Docker-Version >= 20.10 sicherstellen |

---

## 5. Geschätzter Aufwand

| Schritt | Beschreibung |
|---------|-------------|
| Schritt 1 | Zentralisierung validieren |
| Schritt 2 | Versionskatalog (1-3 Einträge) auf 2.0.4 anheben |
| Schritt 3 | Konsistenzprüfung der Imports |
| Schritt 4 | Micronaut-Kompatibilitätscheck |
| Schritt 5-6 | Build/Test + Fehleranalyse |

---

## 6. Referenzen

- [Testcontainers 2.0.0 Release Notes](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.0)
- [OpenRewrite Testcontainers 2.x Migration Recipe](https://docs.openrewrite.org/recipes/java/testing/testcontainers/testcontainers2migration)
- [Testcontainers Changelog](https://github.com/testcontainers/testcontainers-java/blob/main/CHANGELOG.md)
- [Micronaut Test Resources Dokumentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/)
