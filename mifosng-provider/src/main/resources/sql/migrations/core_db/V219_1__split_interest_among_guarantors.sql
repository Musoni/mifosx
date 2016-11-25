CREATE TABLE `m_guarantor_interest_allocation` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`loan_id` BIGINT(20) NOT NULL,
	`allocated_interest_paid` DECIMAL(19,6) NOT NULL,
	`is_reversed` tinyint(1) NOT NULL DEFAULT '0',
	`submitted_user_id` BIGINT(20) NULL,
	`submitted_on_date` DATE NOT NULL,
	`created_on_date` DATETIME NOT NULL,
	 PRIMARY KEY (`id`),
	 CONSTRAINT `FK_m_guarantors_interest_loan` FOREIGN KEY (`loan_id`) REFERENCES `m_loan` (`id`)
);


CREATE TABLE `m_guarantor_loan_interest_payment` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`guarantor_id` BIGINT(20) NOT NULL,
	`guarantor_interest_allocation_id` BIGINT(20) NOT NULL,
	`deposited_amount` DECIMAL(19,6) NOT NULL,
	`savings_account_id` BIGINT(20) NOT NULL,
	`savings_transaction_id` BIGINT(20) NOT NULL,
	`is_reversed` tinyint(1) NOT NULL DEFAULT '0',
	`submitted_on_date` DATE NOT NULL,
	 PRIMARY KEY (`id`),
	 CONSTRAINT `FK_m_guarantors_payment` FOREIGN KEY (`guarantor_id`) REFERENCES `m_guarantor` (`id`),
	 CONSTRAINT `FK_m_guarantor_interest_allocation_payment` FOREIGN KEY (`guarantor_interest_allocation_id`) REFERENCES `m_guarantor_interest_allocation` (`id`),
	  CONSTRAINT `FK_m_savings_account_payment` FOREIGN KEY (`savings_account_id`) REFERENCES `m_savings_account` (`id`),
	 CONSTRAINT `FK_m_savings_account_transaction_payment` FOREIGN KEY (`savings_transaction_id`) REFERENCES `m_savings_account_transaction` (`id`)

);



/*permissions*/

insert into `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES ('portfolio', 'SPLITINTERESTAMONGGUARANTORS_LOAN', 'LOAN', 'SPLITINTERESTAMONGGUARANTORS', 0);
insert into `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES ('portfolio', 'SPLITINTERESTAMONGGUARANTORS_LOAN_CHECKER', 'LOAN', 'SPLITINTERESTAMONGGUARANTORS', 0);




