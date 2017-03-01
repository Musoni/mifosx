insert into m_code (code_name, code_label, is_system_defined)
values ('loanProductGroups', 'loanProductGroups', 1);

insert into m_code (code_name, code_label, is_system_defined)
values ('savingsProductGroups', 'savingsProductGroups', 1);

insert into m_code_value (code_id, code_value, order_position)
values (
	(select id from m_code where code_name = 'loanProductGroups'),
	'Individual Loan',  0);

insert into m_code_value (code_id, code_value, order_position)
values (
	(select id from m_code where code_name = 'loanProductGroups'),
	'Group Loan',  0);

insert into m_code_value (code_id, code_value, order_position)
values (
	(select id from m_code where code_name = 'savingsProductGroups'),
	'Individual Savings',  0);

insert into m_code_value (code_id, code_value, order_position)
values (
	(select id from m_code where code_name = 'savingsProductGroups'),
	'Group Savings',  0);

ALTER TABLE m_product_loan ADD COLUMN product_group bigint(20) NULL;
ALTER TABLE m_savings_product ADD COLUMN product_group bigint(20) NULL;