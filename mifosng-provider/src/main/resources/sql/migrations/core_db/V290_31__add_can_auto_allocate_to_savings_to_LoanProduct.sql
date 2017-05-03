ALTER TABLE m_product_loan ADD COLUMN can_auto_allocate_overpayments TINYINT(1) DEFAULT '0' NOT NULL;

INSERT INTO job ( name, display_name, cron_expression, create_time,
 task_priority, group_name, previous_run_start_time,
 next_run_time, job_key, initializing_errorlog, is_active,
 currently_running, updates_allowed, scheduler_group, is_misfired)
VALUE
( 'Allocate overpayments to savings',
'Allocate overpayments to savings', '0 0 5 * * ?', now(), '5', NULL,
NULL, NULL, 'Allocate overpayments to savingsJobDetail2 _ DEFAULT',
NULL, '1', '0', '1', '0', '0') ;