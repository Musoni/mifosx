CREATE TABLE IF NOT EXISTS `m_savings_product_add_charge_to_existing_accounts` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `savings_product_id` bigint(20) NOT NULL,
  `charge_id` bigint(20) NOT NULL,
  `apply_charge_to_existing_savings_account` TINYINT(1) DEFAULT 0 NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`savings_product_id`) REFERENCES `m_savings_product`(`id`),
  FOREIGN KEY (`charge_id`) REFERENCES `m_charge`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




INSERT INTO job ( name, display_name, cron_expression, create_time,
 task_priority, group_name, previous_run_start_time,
 next_run_time, job_key, initializing_errorlog, is_active,
 currently_running, updates_allowed, scheduler_group, is_misfired)
VALUE
( 'Apply product charge to existing savings account',
'Apply product charge to existing savings account', '0 0 12 1/1 * ? *', now(), '5', NULL,
NULL, NULL, 'Apply savings product charge to existing savings accountJobDetail2 _ DEFAULT',
NULL, '1', '0', '1', '0', '0') ;

