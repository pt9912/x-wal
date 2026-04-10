-- Sprint 3 Task 9.3 follow-up
-- 1. Allow TERMINATED instance status (neutral terminate end events)
-- 2. Add index for keyset pagination used by InstanceStateSyncService

ALTER TABLE iwm_instances
    DROP CONSTRAINT IF EXISTS chk_instance_status;

ALTER TABLE iwm_instances
    ADD CONSTRAINT chk_instance_status
    CHECK (status IN ('RUNNING', 'SUSPENDED', 'COMPLETED', 'FAILED', 'CANCELLED', 'TERMINATED'));

CREATE INDEX IF NOT EXISTS iwm_instances_sync_idx
    ON iwm_instances (updated_at DESC, id DESC);
