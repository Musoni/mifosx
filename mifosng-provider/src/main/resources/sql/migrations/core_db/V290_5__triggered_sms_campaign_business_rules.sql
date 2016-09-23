INSERT INTO `stretchy_report` (`report_name`, `report_type`, `report_subtype`, `report_sql`, `description`, `core_report`, `use_report`)
VALUES ('Loan Rejected', 'Sms', 'Triggered', 'SELECT mc.id, mc.firstname, mc.middlename as middlename, mc.lastname, mc.display_name as FullName, mc.mobile_no as mobileNo, mc.group_name as GroupName,\n mo.name as officename, ifnull(od.phoneNumber,\'\') as officenummber, ml.id as loanId, ml.account_no as accountnumber, ml.principal_amount_proposed as loanamount, ml.annual_nominal_interest_rate as annualinterestrate\n FROM\n m_office mo\n JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, \'%\')\n AND ounder.hierarchy like CONCAT(\'.\', \'%\')\n LEFT JOIN (\n select \n ml.id as loanId, \n ifnull(mc.id,mc2.id) as id, \n ifnull(mc.firstname,mc2.firstname) as firstname, \n ifnull(mc.middlename,ifnull(mc2.middlename,(\'\'))) as middlename, \n ifnull(mc.lastname,mc2.lastname) as lastname, \n ifnull(mc.display_name,mc2.display_name) as display_name, \n ifnull(mc.status_enum,mc2.status_enum) as status_enum,\n ifnull(mc.mobile_no,mc2.mobile_no) as mobile_no,\n ifnull(mg.office_id,mc2.office_id) as office_id,\n ifnull(mg.staff_id,mc2.staff_id) as staff_id,\n mg.id as group_id, \nmg.display_name as group_name\n from\n m_loan ml\n left join m_group mg on mg.id = ml.group_id\n left join m_group_client mgc on mgc.group_id = mg.id\n left join m_client mc on mc.id = mgc.client_id\n left join m_client mc2 on mc2.id = ml.client_id\n order by loanId\n ) mc on mc.office_id = ounder.id\n left join ml_office_details as od on od.office_id = mo.id\n left join m_loan ml on ml.id = mc.loanId\n WHERE mc.status_enum = 300 and mc.mobile_no is not null\n and (mo.id = ${officeId} or ${officeId} = -1)\n and (mc.staff_id = ${staffId} or ${staffId} = -1)\nand (ml.id = ${loanId} or ${loanId} = -1)\nand (mc.id = ${clientId} or ${clientId} = -1)\nand (mc.group_id = ${groupId} or ${groupId} = -1) \nand (ml.loan_type_enum = ${loanType} or ${loanType} = -1)', 'Loan and client data of rejected loan', '0', '0');

INSERT INTO `stretchy_report` (`report_name`, `report_type`, `report_subtype`, `report_sql`, `description`, `core_report`, `use_report`)
VALUES ('Loan Approved', 'Sms', 'Triggered', 'SELECT mc.id, mc.firstname, mc.middlename as middlename, mc.lastname, mc.display_name as FullName, mc.mobile_no as mobileNo, mc.group_name as GroupName,\n mo.name as officename, ifnull(od.phoneNumber,\'\') as officenummber, ml.id as loanId, ml.account_no as accountnumber, ml.principal_amount_proposed as loanamount, ml.annual_nominal_interest_rate as annualinterestrate\n FROM\n m_office mo\n JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, \'%\')\n AND ounder.hierarchy like CONCAT(\'.\', \'%\')\n LEFT JOIN (\n select \n ml.id as loanId, \n ifnull(mc.id,mc2.id) as id, \n ifnull(mc.firstname,mc2.firstname) as firstname, \n ifnull(mc.middlename,ifnull(mc2.middlename,(\'\'))) as middlename, \n ifnull(mc.lastname,mc2.lastname) as lastname, \n ifnull(mc.display_name,mc2.display_name) as display_name, \n ifnull(mc.status_enum,mc2.status_enum) as status_enum,\n ifnull(mc.mobile_no,mc2.mobile_no) as mobile_no,\n ifnull(mg.office_id,mc2.office_id) as office_id,\n ifnull(mg.staff_id,mc2.staff_id) as staff_id,\n mg.id as group_id, \nmg.display_name as group_name\n from\n m_loan ml\n left join m_group mg on mg.id = ml.group_id\n left join m_group_client mgc on mgc.group_id = mg.id\n left join m_client mc on mc.id = mgc.client_id\n left join m_client mc2 on mc2.id = ml.client_id\n order by loanId\n ) mc on mc.office_id = ounder.id\n left join ml_office_details as od on od.office_id = mo.id\n left join m_loan ml on ml.id = mc.loanId\n WHERE mc.status_enum = 300 and mc.mobile_no is not null\n and (mo.id = ${officeId} or ${officeId} = -1)\n and (mc.staff_id = ${staffId} or ${staffId} = -1)\nand (ml.id = ${loanId} or ${loanId} = -1)\nand (mc.id = ${clientId} or ${clientId} = -1)\nand (mc.group_id = ${groupId} or ${groupId} = -1)\nand (ml.loan_type_enum = ${loanType} or ${loanType} = -1)', 'Loan and client data of approved loan', '0', '0');

INSERT INTO `stretchy_report` (`report_name`, `report_type`, `report_subtype`, `report_sql`, `description`, `core_report`, `use_report`)
VALUES ('Loan Repayment', 'Sms', 'Triggered', 'select ml.id as loanId,mc.id, mc.firstname, ifnull(mc.middlename,\'\') as middlename, mc.lastname, mc.display_name as FullName, mobile_no as mobileNo, mc.group_name as GroupName, round(ml.principal_amount, ml.currency_digits) as LoanAmount, round(ml.`total_outstanding_derived`, ml.currency_digits) as LoanOutstanding,\nifnull(od.phoneNumber,\'\') as officenummber,ml.`account_no` as LoanAccountId, round(mlt.amountPaid, ml.currency_digits) as repaymentAmount\nFROM m_office mo\nJOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, \'%\')\nAND ounder.hierarchy like CONCAT(\'.\', \'%\')\nLEFT JOIN (\n select \n ml.id as loanId, \n ifnull(mc.id,mc2.id) as id, \n ifnull(mc.firstname,mc2.firstname) as firstname, \n ifnull(mc.middlename,ifnull(mc2.middlename,(\'\'))) as middlename, \n ifnull(mc.lastname,mc2.lastname) as lastname, \n ifnull(mc.display_name,mc2.display_name) as display_name, \n ifnull(mc.status_enum,mc2.status_enum) as status_enum,\n ifnull(mc.mobile_no,mc2.mobile_no) as mobile_no,\n ifnull(mg.office_id,mc2.office_id) as office_id,\n ifnull(mg.staff_id,mc2.staff_id) as staff_id,\n mg.id as group_id, \nmg.display_name as group_name\n from\n m_loan ml\n left join m_group mg on mg.id = ml.group_id\n left join m_group_client mgc on mgc.group_id = mg.id\n left join m_client mc on mc.id = mgc.client_id\n left join m_client mc2 on mc2.id = ml.client_id\n order by loanId\n ) mc on mc.office_id = ounder.id\nleft join ml_office_details as od on od.office_id = mo.id\nright join m_loan as ml on mc.loanId = ml.id\nright join(\nselect mlt.amount as amountPaid,mlt.id,mlt.loan_id\nfrom m_loan_transaction mlt\nwhere mlt.is_reversed = 0 \ngroup by mlt.loan_id\n) as mlt on mlt.loan_id = ml.id\nright join m_loan_repayment_schedule as mls1 on ml.id = mls1.loan_id and mls1.`completed_derived` = 0\nand mls1.installment = (SELECT MIN(installment) from m_loan_repayment_schedule where loan_id = ml.id and duedate <= CURDATE() and completed_derived=0)\nwhere mc.status_enum = 300 and mobile_no is not null and ml.`loan_status_id` = 300\nand (mo.id = ${officeId} or ${officeId} = -1)\nand (mc.staff_id = ${staffId} or ${staffId} = -1)\nand (ml.loan_type_enum = ${loanType} or ${loanType} = -1)\nand ml.id in (select mla.loan_id from m_loan_arrears_aging mla)\ngroup by ml.id', 'Loan Repayment', '0', '0');

INSERT INTO `r_enum_value` (`enum_name`, `enum_id`, `enum_message_property`, `enum_value`, `enum_type`)
VALUES ('loan_type_enum', '-1', 'All', 'All', '0');
INSERT INTO `r_enum_value` (`enum_name`, `enum_id`, `enum_message_property`, `enum_value`, `enum_type`)
VALUES ('loan_type_enum', '1', 'Individual Loan', 'Individual Loan', '0');
INSERT INTO `r_enum_value` (`enum_name`, `enum_id`, `enum_message_property`, `enum_value`, `enum_type`)
VALUES ('loan_type_enum', '2', 'Group Loan', 'Group Loan', '0');


INSERT INTO `stretchy_parameter` (`parameter_name`, `parameter_variable`, `parameter_label`, `parameter_displayType`, `parameter_FormatType`, `parameter_default`, `selectAll`, `parameter_sql`, `parent_id`)
VALUES ('DefaultLoan', 'loanId', 'Loan', 'none', 'number', '-1', 'Y', 'select ml.id \nfrom m_loan ml \nleft join m_client mc on mc.id = ml.client_id \nleft join m_office mo on mo.id = mc.office_id \nwhere mo.id = ${officeId} or ${officeId} = -1', '5');
INSERT INTO `stretchy_parameter` (`parameter_name`, `parameter_variable`, `parameter_label`, `parameter_displayType`, `parameter_FormatType`, `parameter_default`, `selectAll`, `parameter_sql`, `parent_id`)
VALUES ('DefaultClient', 'clientId', 'Client', 'none', 'number', '-1', 'Y', 'select mc.id \nfrom m_client mc\n left join m_office on mc.office_id = mo.id\n where mo.id = ${officeId} or ${officeId} = -1', '5');
INSERT INTO `stretchy_parameter` (`parameter_name`, `parameter_variable`, `parameter_label`, `parameter_displayType`, `parameter_FormatType`, `parameter_default`, `selectAll`, `parameter_sql`, `parent_id`)
VALUES ('DefaultGroup', 'groupId', 'Group', 'none', 'number', '-1', 'Y', 'select mg.id \nfrom m_group mg\nleft join m_office mo on mg.office_id = mo.id\nwhere mo.id = ${officeId} or ${officeId} = -1', '5');
INSERT INTO `stretchy_parameter` (`parameter_name`, `parameter_variable`, `parameter_label`, `parameter_displayType`, `parameter_FormatType`, `parameter_default`, `selectAll`, `parameter_sql`, `parent_id`)
VALUES ('SelectLoanType', 'loanType', 'Loan Type', 'select', 'number', '-1', 'Y', "select\nenum_id as id,\nenum_value as value\nfrom r_enum_value\nwhere enum_name = 'loan_type_enum'", NULL);

SET @LRej = (select id from `stretchy_report` where `report_name`='Loan Rejected');
SET @LApp = (select id from `stretchy_report` where `report_name`='Loan Approved');
SET @LRep = (select id from `stretchy_report` where `report_name`='Loan Repayment');
SET @Office = (select id from `stretchy_parameter` where `parameter_name`='OfficeIdSelectOne');
SET @loanOfficer = (select id from `stretchy_parameter` where `parameter_name`='LoanOfficerSelectOneRec');
SET @DLoan = (select id from `stretchy_parameter` where `parameter_name`='DefaultLoan');
SET @DClient = (select id from `stretchy_parameter` where `parameter_name`='DefaultClient');
SET @DGroup = (select id from `stretchy_parameter` where `parameter_name`='DefaultGroup');
SET @LoanType = (select id from `stretchy_parameter` where `parameter_name`='SelectLoanType');

INSERT INTO `stretchy_report_parameter` (`report_id`, `parameter_id`, `report_parameter_name`)
VALUES (@LRej, @Office, 'Office'),
(@LApp, @Office, 'Office'),
(@LRep, @Office, 'Office'),
(@LRej, @loanOfficer, 'loanOfficer'),
(@LApp, @loanOfficer, 'loanOfficer'),
(@LRep, @loanOfficer, 'loanOfficer'),
(@LRej, @DLoan, 'loanId'),
(@LApp, @DLoan, 'loanId'),
(@LRej, @DClient, 'clientId'),
(@LApp, @DClient, 'clientId'),
(@LRej, @DGroup, 'groupId'),
(@LApp, @DGroup, 'groupId'),
(@LRej, @LoanType, 'loanType'),
(@LApp, @LoanType, 'loanType'),
(@LRep, @LoanType, 'loanType');

