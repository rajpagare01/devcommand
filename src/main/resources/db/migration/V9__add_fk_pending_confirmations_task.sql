-- Phase 5 hardening: add FK from telegram_pending_confirmations.task_id to daily_tasks.id.
-- ON DELETE CASCADE ensures dangling confirmation tokens are cleaned up automatically
-- when the referenced task is deleted outside the confirmation flow.
--
-- Wrapped in a DO block to be idempotent (safe if constraint already exists or table
-- was dropped and re-created by test frameworks using hibernate ddl-auto=create-drop).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'telegram_pending_confirmations'
    )
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_pending_confirmations_task'
          AND table_name   = 'telegram_pending_confirmations'
    ) THEN
        ALTER TABLE telegram_pending_confirmations
            ADD CONSTRAINT fk_pending_confirmations_task
            FOREIGN KEY (task_id) REFERENCES daily_tasks (id) ON DELETE CASCADE;
    END IF;
END $$;
