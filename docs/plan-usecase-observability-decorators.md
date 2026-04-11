# Plan: Use-Case-Level Tracing/Metrics für alle Use Cases abschliessen

## Ziel
Alle 19 Use Cases sollen nach einem einheitlichen Muster für:
- Tracing via `TracedUseCaseDecorator`
- Metrics via `MeteredUseCaseDecorator`

ausgehändigt werden.

Das Ziel ist:
- volle Beobachtbarkeit auf Use-Case-Ebene
- konsistente Operation-Namen
- reproduzierbare Metriken/Spans (Fehler inkl. Error-Tagging)
- Abnahmekriterium `Use-Case-Level Tracing/Metrics` in `docs/MVP-Plan-2.0.0.md`.

## Ist-Stand (Code-Stand)

- Vollständig dekoriert:  
  - `CreateWorkflowUseCase`
  - `StartWorkflowUseCase`
  - `SuspendInstanceUseCase`
  - `ResumeInstanceUseCase`
- Teilweise dekoriert/Inkonsistent:
  - `CancelInstanceUseCase` (manuelle Span/Counter-Logik statt der Standard-Decorator)
- Keine Dekoration:
  - `GetWorkflowUseCase`
  - `ListWorkflowsUseCase`
  - `UpdateWorkflowUseCase`
  - `DeleteWorkflowUseCase`
  - `GetInstanceUseCase`
  - `GetInstanceVariablesUseCase`
  - `QueryTasksUseCase`
  - `AssignTaskUseCase`
  - `GetTaskUseCase`
  - `RegisterAdapterUseCase`
  - `DeregisterAdapterUseCase`
  - `HealthCheckAdapterUseCase`
  - `SyncInstanceStateUseCase`

## Vorgehen

1. Zielbild in den Factories definieren
   - Verwendung der bestehenden Decorator-Typen:
     - `com.xwal.decorator.TracedUseCaseDecorator`
     - `com.xwal.decorator.MeteredUseCaseDecorator`
   - Operation Names im Format `domain.action` weiterführen.
   - Bei allen 19 Use Cases Reihenfolge `traced -> metered -> impl`.

2. `WorkflowUseCaseFactory` vereinheitlichen
   - `getWorkflowUseCase`, `listWorkflowsUseCase`, `updateWorkflowUseCase`, `deleteWorkflowUseCase`, `getInstanceUseCase`, `getInstanceVariablesUseCase` auf Decorator-Kette umstellen.
   - `cancelInstanceUseCase` von manueller Implementierung auf Standard-Dekorator-Muster umbauen.

3. `TaskUseCaseFactory` vereinheitlichen
   - `assignTaskUseCase` und `getTaskUseCase` mit `TracedUseCaseDecorator` + `MeteredUseCaseDecorator` ausstatten.
   - `completeTaskUseCase` auf identische Kette wie `queryTasksUseCase` umbauen.

4. `AdapterUseCaseFactory` vereinheitlichen
   - `registerAdapterUseCase`, `deregisterAdapterUseCase`, `healthCheckAdapterUseCase`, `syncInstanceStateUseCase` mit beiden Decorators versehen.
   - `syncInstanceStateUseCase` behält bestehende Parameter (`batchSize`, `timeout`, `retry`) unverändert.

5. Metrik-/Tracing-Namensschema abstimmen
   - Standardisierte Operationen je Use Case:
     - `workflow.create`, `workflow.get`, `workflow.list`, `workflow.update`, `workflow.delete`
     - `workflow.start_instance`, `workflow.get_instance`, `workflow.instance_variables`
     - `workflow.suspend`, `workflow.resume`, `workflow.cancel`
     - `task.query`, `task.complete`, `task.assign`, `task.get`
     - `adapter.register`, `adapter.deregister`, `adapter.healthcheck`, `adapter.sync_instances`
   - In `docs/hexagonal-migration.md` nur ändern, falls dort benannte Metriken referenziert werden.

6. Tests ergänzen
   - Neue Testfälle in `app/src/test/kotlin/com/xwal/factory/FactoryWiringTest.kt`:
     - Für jeden bisher un-dekorierten Use Case: Ausführung mit Mock-Span/Mock-Counter-Hook prüfen.
     - Erwartung: Tracer und Meter werden mindestens einmal aufgerufen.
   - Regressionstest für `Cancel`/`Complete`:
     - bestehende manuelle Pfade entfernen, gleicher Interceptor-Flow wie Standard-Dekoratoren.

7. Dokumentation + Abnahme aktualisieren
   - `docs/MVP-Plan-2.0.0.md`:
     - Haken `Use-Case Observability-Decorators` auf `[x]` setzen.
     - Abnahmekriterium `Use-Case-Level Tracing/Metrics` auf `[x]` setzen.
   - Optional: Hinweis im Abschnitt „API-Umsetzungsstand“, falls Monitoring als Folgeaufgabe dokumentiert war.

## Reihenfolge

1. Factory-Anpassungen (`Workflow`, `Task`, `Adapter`) in einem PR.
2. Tests (`FactoryWiringTest`) direkt im Anschluss.
3. Kurzer Smoke-Run der App + API-Tests.
4. Dokumente nachziehen und committen.

## Akzeptanzkriterien

- Alle 19 Use Cases sind über eine einheitliche `traced + metered` Kette verdrahtet.
- Kein Use Case verbleibt nur mit manueller Span/Counter-Logik.
- `docs/MVP-Plan-2.0.0.md` zeigt die Aufgabe als erledigt.
- Es gibt keine regressiven Änderungen an 77 bestehenden Tests.
