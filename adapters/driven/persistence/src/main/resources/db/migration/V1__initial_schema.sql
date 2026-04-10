-- X-WAL v1 Initial Schema
-- Sprint 2 - Persistenz-Schicht
--
-- Tabellen:
-- 1. iwm_workflows - Speichert IWM-Workflow-Definitionen
-- 2. iwm_instances - Speichert laufende Workflow-Instanzen
-- 3. engine_adapters - Speichert Engine-Adapter-Konfigurationen

-- =====================================================
-- 1. IWM Workflows Table
-- =====================================================
CREATE TABLE iwm_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Workflow Metadata (aus IWM workflow-Objekt)
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    description TEXT,

    -- IWM Definition als JSONB (gesamtes IWM-Dokument)
    iwm_definition JSONB NOT NULL,

    -- Status & Lifecycle
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, DEPRECATED, ARCHIVED

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255),

    -- Constraints
    CONSTRAINT uk_workflow_name_version UNIQUE(name, version),
    CONSTRAINT chk_workflow_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED', 'ARCHIVED'))
);

-- Indizes für Queries
CREATE INDEX idx_iwm_workflows_name ON iwm_workflows(name);
CREATE INDEX idx_iwm_workflows_status ON iwm_workflows(status);
CREATE INDEX idx_iwm_workflows_created_at ON iwm_workflows(created_at);

-- JSONB GIN Index für schnelle Suchen in IWM-Definition
CREATE INDEX idx_iwm_workflows_definition ON iwm_workflows USING GIN(iwm_definition);

-- =====================================================
-- 2. IWM Instances Table
-- =====================================================
CREATE TABLE iwm_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Referenz zum Workflow
    workflow_id UUID NOT NULL REFERENCES iwm_workflows(id),

    -- Engine-spezifische Instance ID (z.B. Camunda Process Instance ID)
    engine_instance_id VARCHAR(255) NOT NULL,

    -- Engine Adapter Referenz
    engine_adapter_id UUID NOT NULL, -- wird später zur engine_adapters Tabelle verlinkt

    -- Instance Status
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING', -- RUNNING, SUSPENDED, COMPLETED, FAILED, CANCELLED

    -- Business Key (optional, für fachliche Referenz)
    business_key VARCHAR(255),

    -- Variables (JSONB für Workflow-Variablen)
    variables JSONB,

    -- Timestamps
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_by VARCHAR(255),
    ended_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_instance_status CHECK (status IN ('RUNNING', 'SUSPENDED', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Indizes für Queries
CREATE INDEX idx_iwm_instances_workflow_id ON iwm_instances(workflow_id);
CREATE INDEX idx_iwm_instances_engine_instance_id ON iwm_instances(engine_instance_id);
CREATE INDEX idx_iwm_instances_engine_adapter_id ON iwm_instances(engine_adapter_id);
CREATE INDEX idx_iwm_instances_status ON iwm_instances(status);
CREATE INDEX idx_iwm_instances_business_key ON iwm_instances(business_key) WHERE business_key IS NOT NULL;
CREATE INDEX idx_iwm_instances_started_at ON iwm_instances(started_at);

-- JSONB GIN Index für Variablen-Suche
CREATE INDEX idx_iwm_instances_variables ON iwm_instances USING GIN(variables);

-- =====================================================
-- 3. Engine Adapters Table
-- =====================================================
CREATE TABLE engine_adapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Adapter Identification
    name VARCHAR(255) NOT NULL UNIQUE,
    engine_type VARCHAR(50) NOT NULL, -- CAMUNDA7, CAMUNDA8, FLOWABLE, TEMPORAL, etc.

    -- Connection Configuration (JSONB für flexibles Config-Schema)
    config JSONB NOT NULL,
    -- Beispiel config:
    -- {
    --   "url": "http://camunda:8080/engine-rest",
    --   "auth": {"type": "basic", "username": "demo", "password": "..."},
    --   "timeout": 30000
    -- }

    -- Capabilities & Features
    capabilities JSONB, -- z.B. {"supports": ["userTasks", "messageEvents", "timers"]}

    -- Status
    enabled BOOLEAN NOT NULL DEFAULT true,
    health_status VARCHAR(50) DEFAULT 'UNKNOWN', -- HEALTHY, UNHEALTHY, UNKNOWN
    last_health_check TIMESTAMP WITH TIME ZONE,

    -- Priority für Routing (höher = bevorzugt)
    priority INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255),

    -- Constraints
    CONSTRAINT chk_engine_type CHECK (engine_type IN ('CAMUNDA7', 'CAMUNDA8', 'FLOWABLE', 'TEMPORAL', 'CONDUCTOR')),
    CONSTRAINT chk_health_status CHECK (health_status IN ('HEALTHY', 'UNHEALTHY', 'UNKNOWN'))
);

-- Indizes
CREATE INDEX idx_engine_adapters_engine_type ON engine_adapters(engine_type);
CREATE INDEX idx_engine_adapters_enabled ON engine_adapters(enabled);
CREATE INDEX idx_engine_adapters_priority ON engine_adapters(priority DESC);

-- JSONB GIN Index für Config & Capabilities
CREATE INDEX idx_engine_adapters_config ON engine_adapters USING GIN(config);
CREATE INDEX idx_engine_adapters_capabilities ON engine_adapters USING GIN(capabilities);

-- =====================================================
-- Foreign Key für engine_adapter_id in iwm_instances
-- =====================================================
ALTER TABLE iwm_instances
    ADD CONSTRAINT fk_instances_engine_adapter
    FOREIGN KEY (engine_adapter_id) REFERENCES engine_adapters(id);

-- =====================================================
-- Trigger für updated_at Timestamps
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_iwm_workflows_updated_at
    BEFORE UPDATE ON iwm_workflows
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_iwm_instances_updated_at
    BEFORE UPDATE ON iwm_instances
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_engine_adapters_updated_at
    BEFORE UPDATE ON engine_adapters
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Initial Data: Default Engine Adapter (Demo)
-- =====================================================
INSERT INTO engine_adapters (name, engine_type, config, capabilities, enabled, priority, health_status)
VALUES (
    'camunda7-local',
    'CAMUNDA7',
    '{"url": "http://localhost:8080/engine-rest", "auth": {"type": "none"}}'::JSONB,
    '{"supports": ["userTasks", "serviceTask", "messageEvents", "timers", "signalEvents"]}'::JSONB,
    true,
    100,
    'HEALTHY'
);

-- =====================================================
-- Comments für Dokumentation
-- =====================================================
COMMENT ON TABLE iwm_workflows IS 'Speichert IWM-Workflow-Definitionen mit vollständigem IWM-JSON in JSONB';
COMMENT ON COLUMN iwm_workflows.iwm_definition IS 'Vollständiges IWM-Dokument als JSONB (tasks, edges, layouts, extensions, meta)';
COMMENT ON COLUMN iwm_workflows.status IS 'Lifecycle-Status: DRAFT (in Bearbeitung), ACTIVE (produktiv), DEPRECATED (veraltet), ARCHIVED (archiviert)';

COMMENT ON TABLE iwm_instances IS 'Speichert laufende Workflow-Instanzen mit Referenz zur Engine';
COMMENT ON COLUMN iwm_instances.engine_instance_id IS 'Engine-spezifische Instance ID (z.B. Camunda Process Instance ID)';
COMMENT ON COLUMN iwm_instances.variables IS 'Workflow-Variablen als JSONB (synchronisiert mit Engine)';

COMMENT ON TABLE engine_adapters IS 'Konfiguration der verfügbaren Workflow-Engine-Adapter';
COMMENT ON COLUMN engine_adapters.config IS 'Engine-Verbindungskonfiguration (URL, Auth, Timeouts) als JSONB';
COMMENT ON COLUMN engine_adapters.capabilities IS 'Engine-Capabilities (unterstützte Features) als JSONB';
COMMENT ON COLUMN engine_adapters.priority IS 'Routing-Priorität (höher = bevorzugt bei mehreren Engines)';
