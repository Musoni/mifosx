INSERT INTO `m_code` (`code_name`, `code_label`, `is_system_defined`)
VALUES ('LoanSubStatus', 'LoanSubStatus', 1);

ALTER TABLE `m_loan` ADD COLUMN `sub_status_cv_id` SMALLINT(5) NULL AFTER `loan_type_enum`;

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES ('portfolio', 'UPDATESUBSTATUS_LOAN', 'LOAN', 'UPDATESUBSTATUS', 0);