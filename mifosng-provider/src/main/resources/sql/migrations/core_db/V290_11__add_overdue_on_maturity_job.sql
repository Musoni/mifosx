INSERT INTO job ( name, display_name, cron_expression, create_time,
 task_priority, group_name, previous_run_start_time, 
 next_run_time, job_key, initializing_errorlog, is_active, 
 currently_running, updates_allowed, scheduler_group, is_misfired)
VALUE
( 'Apply penalty to overdue on maturity loans', 
'Apply penalty to overdue on maturity loans', '0 0 12 * * ?', '2013-11-11 00:00:00', '5', NULL, 
'2016-10-19 10:00:00', '2016-10-20 10:00:00', 'Apply penalty to overdue on maturity loansJobDetail1 _ DEFAULT', 
NULL, '1', '0', '1', '0', '0') ;