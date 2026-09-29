-- MySQL does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS. Use
-- metadata-driven statements so the migration also accepts the trainer's
-- preloaded reference schema.
SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'professional_slots'
      AND COLUMN_NAME = 'reschedule_request_id'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE professional_slots ADD COLUMN reschedule_request_id BIGINT NULL',
    'SELECT 1');
PREPARE add_column FROM @sql;
EXECUTE add_column;
DEALLOCATE PREPARE add_column;

SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'professional_slots'
      AND INDEX_NAME = 'ix_slot_reschedule_request'
);
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX ix_slot_reschedule_request ON professional_slots(reschedule_request_id)',
    'SELECT 1');
PREPARE add_index FROM @sql;
EXECUTE add_index;
DEALLOCATE PREPARE add_index;
