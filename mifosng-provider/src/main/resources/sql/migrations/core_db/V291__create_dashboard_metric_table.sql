CREATE TABLE `m_dashboard_metric_result` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `run_date` DATETIME NOT NULL,
  `metric_name` VARCHAR(250) NOT NULL,
  `metric_value` DECIMAL(19,6) NULL,
  `office_id` BIGINT(20) NULL,
  `staff_id` BIGINT(20) NULL,
  `month_year` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`))ENGINE=InnoDB DEFAULT CHARSET=utf8;