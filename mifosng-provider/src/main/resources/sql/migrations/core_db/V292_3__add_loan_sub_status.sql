INSERT INTO `m_code` (`code_name`, `code_label`, `is_system_defined`)
VALUES ('LoanSubStatus', 'LoanSubStatus', 1);

ALTER TABLE `m_loan` ADD COLUMN `sub_status_cv_id` SMALLINT(5) NULL AFTER `loan_type_enum`;