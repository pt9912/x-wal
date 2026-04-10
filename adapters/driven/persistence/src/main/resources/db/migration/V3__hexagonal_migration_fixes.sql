-- V3: Fixes for hexagonal architecture migration
-- 1. Allow NULL for engine_instance_id, engine_adapter_id, started_at (saga pattern: instance created before engine dispatch)
-- 2. Add UNKNOWN to instance status CHECK constraint
-- 3. Add ACTIVITI to engine_type CHECK constraint

-- 1. Relax NOT NULL constraints on iwm_instances
ALTER TABLE iwm_instances ALTER COLUMN engine_instance_id DROP NOT NULL;
ALTER TABLE iwm_instances ALTER COLUMN engine_adapter_id DROP NOT NULL;
ALTER TABLE iwm_instances ALTER COLUMN started_at DROP NOT NULL;

-- 2. Update instance status CHECK to include UNKNOWN
ALTER TABLE iwm_instances DROP CONSTRAINT IF EXISTS chk_instance_status;
ALTER TABLE iwm_instances ADD CONSTRAINT chk_instance_status
    CHECK (status IN ('RUNNING', 'SUSPENDED', 'COMPLETED', 'FAILED', 'CANCELLED', 'TERMINATED', 'UNKNOWN'));

-- 3. Update engine_type CHECK to include ACTIVITI
ALTER TABLE engine_adapters DROP CONSTRAINT IF EXISTS chk_engine_type;
ALTER TABLE engine_adapters ADD CONSTRAINT chk_engine_type
    CHECK (engine_type IN ('CAMUNDA7', 'CAMUNDA8', 'FLOWABLE', 'ACTIVITI', 'TEMPORAL', 'CONDUCTOR'));
