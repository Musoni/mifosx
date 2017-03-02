insert IGNORE into  c_external_service_properties (name, value,external_service_id)
values("sms_credit_email_reminder", "100",(select id from c_external_service where name ="SMTP_Email_Account")),
("target_email", '',(select id from c_external_service where name ="SMTP_Email_Account"));


insert IGNORE into m_permission(grouping, code, entity_name, action_name, can_maker_checker) values('externalservices', 'READ_EXTERNALSERVICES', 'EXTERNALSERVICES', 'READ', 0);
