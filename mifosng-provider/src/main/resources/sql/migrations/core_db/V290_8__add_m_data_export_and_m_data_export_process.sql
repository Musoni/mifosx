create table if not exists `m_data_export` (
id bigint(20) primary key auto_increment,
base_entity_name varchar(20) not null,
user_request_map text,
data_sql text not null,
is_deleted tinyint(1) default 0 not null,
filename varchar(20),
file_download_count int not null default 0,
createdby_id bigint not null,
created_date datetime not null,
lastmodifiedby_id bigint,
lastmodified_date datetime,
foreign key (createdby_id) references m_appuser(id),
foreign key (lastmodifiedby_id) references m_appuser(id)
);

INSERT INTO `m_permission`
(`grouping`,`code`,`entity_name`,`action_name`,`can_maker_checker`) VALUES
('dataexport','CREATE_DATAEXPORT','DATAEXPORT','CREATE',0),
('dataexport','READ_DATAEXPORT','DATAEXPORT','READ',0),
('dataexport','DELETE_DATAEXPORT','DATAEXPORT','DELETE',0),
('dataexport','CREATE_DATAEXPORTPROCESS','DATAEXPORTPROCESS','CREATE',0),
('dataexport','READ_DATAEXPORTPROCESS','DATAEXPORTPROCESS','READ',0),
('dataexport','DELETE_DATAEXPORTPROCESS','DATAEXPORTPROCESS','DELETE',0);

CREATE TABLE if NOT EXISTS `r_entity_label` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `entity_table` VARCHAR(100) NOT NULL,
  `entity_field_name` VARCHAR(100) NOT NULL,
  `entity_json_param` VARCHAR(100) NOT NULL,
  `entity_field_label` VARCHAR(100) NOT NULL,
  `refers_to_table` VARCHAR(100) NULL,
  `refers_to_field` VARCHAR(100) NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('1', 'm_client', 'account_no', 'accountNo', 'Account');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('2', 'm_client', 'office_id', 'office', 'Office', 'm_office', 'name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('3', 'm_client', 'transfer_to_office_id', 'transferToOffice', 'Transfer To Office');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('4', 'm_client', 'image_id', 'image', 'Image');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('5', 'm_client', 'status_enum', 'status', 'Status');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('6', 'm_client', 'sub_status', 'subStatus', 'Sub Status');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('7', 'm_client', 'activation_date', 'activationDate', 'Activation Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('8', 'm_client', 'office_joining_date', 'officeJoiningDate', 'Office Joining Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('9', 'm_client', 'firstname', 'firstname', 'Firstname');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('10', 'm_client', 'middlename', 'middlename', 'Middlename');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('11', 'm_client', 'lastname', 'lastname', 'Lastname');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('12', 'm_client', 'fullname', 'fullname', 'Fullname');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('13', 'm_client', 'display_name', 'entityName', 'Name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('14', 'm_client', 'mobile_no', 'mobileNo', 'Mobile');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('15', 'm_client', 'external_id', 'externalId', 'External Id');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('16', 'm_client', 'date_of_birth', 'dateOfBirth', 'Date Of Birth');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('17', 'm_client', 'gender_cv_id', 'gender', 'Gender');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('18', 'm_client', 'staff_id', 'staff', 'Staff', 'm_staff', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('19', 'm_client', 'closure_reason_cv_id', 'closureReason', 'Closure Reason');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('20', 'm_client', 'closedon_date', 'closureDate', 'Closure Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('21', 'm_client', 'reject_reason_cv_id', 'rejectionReason', 'Rejection Reason');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('22', 'm_client', 'rejectedon_date', 'rejectionDate', 'Rejection Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('23', 'm_client', 'rejectedon_userid', 'rejectedBy', 'Rejected By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('24', 'm_client', 'withdraw_reason_cv_id', 'withdrawalReason', 'Withdrawal Reason');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('25', 'm_client', 'withdrawn_on_date', 'withdrawalDate', 'Withdrawal Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('26', 'm_client', 'withdraw_on_userid', 'withdrawnBy', 'Withdrawn By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('27', 'm_client', 'reactivated_on_date', 'reactivateDate', 'Reactivate Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('28', 'm_client', 'reactivated_on_userid', 'reactivatedBy', 'Reactivated By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('29', 'm_client', 'closedon_userid', 'closedBy', 'Closed By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('30', 'm_client', 'submittedon_date', 'submittedondate', 'Submitted On');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('31', 'm_client', 'submittedon_userid', 'submittedBy', 'Submitted By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('32', 'm_client', 'updated_on', 'updatedOnDate', 'Updated On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('33', 'm_client', 'updated_by', 'updatedBy', 'Updated By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('34', 'm_client', 'activatedon_userid', 'activatedBy', 'Activated By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('35', 'm_client', 'default_savings_product', 'savingsProduct', 'Savings Product');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('36', 'm_client', 'default_savings_account', 'savingsAccount', 'Savings Account');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('37', 'm_client', 'client_type_cv_id', 'clientType', 'Client Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('38', 'm_client', 'client_classification_cv_id', 'clientClassification', 'Client Classification');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('39', 'm_client', 'legal_form_enum', 'legalForm', 'Legal Form');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('40', 'm_group', 'external_id', 'externalId', 'External Id');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('41', 'm_group', 'status_enum', 'status', 'Status');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('42', 'm_group', 'activation_date', 'activationDate', 'Activation Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('43', 'm_group', 'activatedon_userid', 'activatedBy', 'Activated By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('44', 'm_group', 'office_id', 'office', 'Office', 'm_office', 'name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('45', 'm_group', 'staff_id', 'staff', 'Staff', 'm_staff', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('46', 'm_group', 'parent_id', 'parent', 'Parent');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('47', 'm_group', 'level_id', 'groupLevel', 'Group Level');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('48', 'm_group', 'display_name', 'entityName', 'Name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('49', 'm_group', 'hierarchy', 'hierarchy', 'Hierarchy');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('50', 'm_group', 'parent_id', 'groupMembers', 'Group Members');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('51', 'm_group', 'closure_reason_cv_id', 'closureReason', 'Closure Reason');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('52', 'm_group', 'closedon_date', 'closureDate', 'Closure Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('53', 'm_group', 'closedon_userid', 'closedBy', 'Closed By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('54', 'm_group', 'submittedon_date', 'submittedondate', 'Submitted On');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('55', 'm_group', 'submittedon_userid', 'submittedBy', 'Submitted By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('56', 'm_group', 'account_no', 'accountNo', 'Account');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('57', 'm_loan', 'account_no', 'accountNo', 'Account');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('58', 'm_loan', 'external_id', 'externalId', 'External Id');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('59', 'm_loan', 'client_id', 'clientId', 'Client', 'm_client', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('60', 'm_loan', 'group_id', 'groupId', 'Group', 'm_group', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('61', 'm_loan', 'loan_type_enum', 'type', 'Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('62', 'm_loan', 'product_id', 'type', 'Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('63', 'm_loan', 'fund_id', 'fund', 'Fund');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('64', 'm_loan', 'loan_officer_id', 'loanOfficer', 'Loan Officer');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('65', 'm_loan', 'loanpurpose_cv_id', 'loanPurpose', 'Loan Purpose');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('66', 'm_loan', 'loan_transaction_strategy_id', 'transactionProcessingStrategy', 'Transaction Processing Strategy');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('67', 'm_loan', 'repayment_frequency_nth_day_enum', 'repaymentFrequencyNthDayType', 'Repayment Frequency Nth Day Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('68', 'm_loan', 'repayment_frequency_day_of_week_enum', 'repaymentFrequencyDayOfWeekType', 'Repayment Frequency Day Of Week Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('69', 'm_loan', 'term_frequency', 'termFrequency', 'Term Frequency');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('70', 'm_loan', 'term_period_frequency_enum', 'termPeriodFrequencyType', 'Term Period Frequency Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('71', 'm_loan', 'loan_status_id', 'status', 'Status');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('72', 'm_loan', 'sync_disbursement_with_meeting', 'syncDisbursementWithMeeting', 'Sync Disbursement With Meeting');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('73', 'm_loan', 'submittedon_date', 'submittedondate', 'Submitted On');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('74', 'm_loan', 'submittedon_userid', 'submittedBy', 'Submitted By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('75', 'm_loan', 'rejectedon_date', 'rejectedOnDate', 'Rejected On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('76', 'm_loan', 'rejectedon_userid', 'rejectedBy', 'Rejected By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('77', 'm_loan', 'withdrawnon_date', 'withdrawnOnDate', 'Withdrawn On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('78', 'm_loan', 'withdrawnon_userid', 'withdrawnBy', 'Withdrawn By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('79', 'm_loan', 'approvedon_date', 'approvedOnDate', 'Approved On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('80', 'm_loan', 'approvedon_userid', 'approvedBy', 'Approved By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('81', 'm_loan', 'expected_disbursedon_date', 'expectedDisbursementDate', 'Expected Disbursement Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('82', 'm_loan', 'disbursedon_date', 'actualDisbursementDate', 'Actual Disbursement Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('83', 'm_loan', 'disbursedon_userid', 'disbursedBy', 'Disbursed By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('84', 'm_loan', 'closedon_date', 'closedOnDate', 'Closed On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('85', 'm_loan', 'closedon_userid', 'closedBy', 'Closed By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('86', 'm_loan', 'writtenoffon_date', 'writtenOffOnDate', 'Written Off On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('87', 'm_loan', 'rescheduledon_date', 'rescheduledOnDate', 'Rescheduled On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('88', 'm_loan', 'rescheduledon_userid', 'rescheduledByUser', 'Rescheduled By User');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('89', 'm_loan', 'expected_maturedon_date', 'expectedMaturityDate', 'Expected Maturity Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('90', 'm_loan', 'maturedon_date', 'actualMaturityDate', 'Actual Maturity Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('91', 'm_loan', 'expected_firstrepaymenton_date', 'expectedFirstRepaymentOnDate', 'Expected First Repayment On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('92', 'm_loan', 'interest_calculated_from_date', 'interestChargedFromDate', 'Interest Charged From Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('93', 'm_loan', 'total_overpaid_derived', 'totalOverpaid', 'Total Overpaid');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('94', 'm_loan', 'loan_counter', 'loanCounter', 'Loan Counter');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('95', 'm_loan', 'loan_product_counter', 'loanProductCounter', 'Loan Product Counter');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('96', 'm_loan', 'principal_amount_proposed', 'proposedPrincipal', 'Proposed Principal');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('97', 'm_loan', 'approved_principal', 'approvedPrincipal', 'Approved Principal');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('98', 'm_loan', 'fixed_emi_amount', 'fixedEmiAmount', 'Fixed Emi Amount');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('99', 'm_loan', 'max_outstanding_loan_balance', 'maxOutstandingLoanBalance', 'Max Outstanding Loan Balance');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('100', 'm_loan', 'total_recovered_derived', 'totalRecovered', 'Total Recovered');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('101', 'm_loan', 'is_npa', 'isNpa', 'Is Npa');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('102', 'm_loan', 'is_suspended_income', 'isSuspendedIncome', 'Is Suspended Income');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('103', 'm_loan', 'accrued_till', 'accruedTill', 'Accrued Till');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('104', 'm_loan', 'create_standing_instruction_at_disbursement', 'createStandingInstructionAtDisbursement', 'Create Standing Instruction At Disbursement');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('105', 'm_loan', 'guarantee_amount_derived', 'guaranteeAmountDerived', 'Guarantee Amount Derived');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('106', 'm_loan', 'interest_recalcualated_on', 'interestRecalculatedOn', 'Interest Recalculated On');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('107', 'm_loan', 'is_floating_interest_rate', 'isFloatingInterestRate', 'Is Floating Interest Rate');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('108', 'm_loan', 'interest_rate_differential', 'interestRateDifferential', 'Interest Rate Differential');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('109', 'm_savings_account', 'account_no', 'accountNo', 'Account');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('110', 'm_savings_account', 'external_id', 'externalId', 'External Id');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('111', 'm_savings_account', 'client_id', 'clientId', 'Client', 'm_client', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`, `refers_to_table`, `refers_to_field`) VALUES ('112', 'm_savings_account', 'group_id', 'groupId', 'Group', 'm_group', 'display_name');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('113', 'm_savings_account', 'product_id', 'type', 'Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('114', 'm_savings_account', 'field_officer_id', 'savingsOfficer', 'Savings Officer');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('115', 'm_savings_account', 'status_enum', 'status', 'Status');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('116', 'm_savings_account', 'account_type_enum', 'accountType', 'Account Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('117', 'm_savings_account', 'submittedon_date', 'submittedondate', 'Submitted On');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('118', 'm_savings_account', 'submittedon_userid', 'submittedBy', 'Submitted By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('119', 'm_savings_account', 'rejectedon_date', 'rejectedOnDate', 'Rejected On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('120', 'm_savings_account', 'rejectedon_userid', 'rejectedBy', 'Rejected By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('121', 'm_savings_account', 'withdrawnon_date', 'withdrawnOnDate', 'Withdrawn On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('122', 'm_savings_account', 'withdrawnon_userid', 'withdrawnBy', 'Withdrawn By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('123', 'm_savings_account', 'approvedon_date', 'approvedOnDate', 'Approved On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('124', 'm_savings_account', 'approvedon_userid', 'approvedBy', 'Approved By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('125', 'm_savings_account', 'activatedon_date', 'activatedOnDate', 'Activated On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('126', 'm_savings_account', 'activatedon_userid', 'activatedBy', 'Activated By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('127', 'm_savings_account', 'closedon_date', 'closedOnDate', 'Closed On Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('128', 'm_savings_account', 'closedon_userid', 'closedBy', 'Closed By');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('129', 'm_savings_account', 'nominal_annual_interest_rate', 'nominalAnnualInterestRate', 'Nominal Annual Interest Rate');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('130', 'm_savings_account', 'interest_compounding_period_enum', 'interestCompoundingPeriodType', 'Interest Compounding Period Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('131', 'm_savings_account', 'interest_posting_period_enum', 'interestPostingPeriodType', 'Interest Posting Period Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('132', 'm_savings_account', 'interest_calculation_type_enum', 'interestCalculationType', 'Interest Calculation Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('133', 'm_savings_account', 'interest_calculation_days_in_year_type_enum', 'interestCalculationDaysInYearType', 'Interest Calculation Days In Year Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('134', 'm_savings_account', 'min_required_opening_balance', 'minRequiredOpeningBalance', 'Min Required Opening Balance');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('135', 'm_savings_account', 'lockin_period_frequency', 'lockinPeriodFrequency', 'Lockin Period Frequency');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('136', 'm_savings_account', 'lockin_period_frequency_enum', 'lockinPeriodFrequencyType', 'Lockin Period Frequency Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('137', 'm_savings_account', 'lockedin_until_date_derived', 'lockedInUntilDate', 'Locked In Until Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('138', 'm_savings_account', 'withdrawal_fee_for_transfer', 'withdrawalFeeApplicableForTransfer', 'Withdrawal Fee Applicable For Transfer');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('139', 'm_savings_account', 'allow_overdraft', 'allowOverdraft', 'Allow Overdraft');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('140', 'm_savings_account', 'overdraft_limit', 'overdraftLimit', 'Overdraft Limit');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('141', 'm_savings_account', 'nominal_annual_interest_rate_overdraft', 'nominalAnnualInterestRateOverdraft', 'Nominal Annual Interest Rate Overdraft');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('142', 'm_savings_account', 'min_overdraft_for_interest_calculation', 'minOverdraftForInterestCalculation', 'Min Overdraft For Interest Calculation');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('143', 'm_savings_account', 'enforce_min_required_balance', 'enforceMinRequiredBalance', 'Enforce Min Required Balance');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('144', 'm_savings_account', 'min_required_balance', 'minRequiredBalance', 'Min Required Balance');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('145', 'm_savings_account', 'on_hold_funds_derived', 'onHoldFunds', 'On Hold Funds');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('146', 'm_savings_account', 'start_interest_calculation_date', 'startInterestCalculationDate', 'Start Interest Calculation Date');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('147', 'm_savings_account', 'deposit_type_enum', 'depositType', 'Deposit Type');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('148', 'm_savings_account', 'min_balance_for_interest_calculation', 'minBalanceForInterestCalculation', 'Min Balance For Interest Calculation');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('149', 'm_client', 'id', 'id', 'ID');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('150', 'm_group', 'id', 'id', 'ID');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('151', 'm_loan', 'id', 'id', 'ID');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('152', 'm_savings_account', 'id', 'id', 'ID');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('153', 'm_loan', 'principal_amount', 'principal', 'Principal');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('154', 'm_loan', 'total_outstanding_derived', 'outstanding', 'Outstanding');
INSERT INTO `r_entity_label` (`id`, `entity_table`, `entity_field_name`, `entity_json_param`, `entity_field_label`) VALUES ('155', 'm_savings_account', 'account_balance_derived', 'accountBalance', 'Account Balance');