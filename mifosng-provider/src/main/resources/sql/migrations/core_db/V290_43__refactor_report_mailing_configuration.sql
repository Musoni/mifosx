INSERT IGNORE INTO `m_report_mailing_job_configuration` (`id`, `name`, `value`)
VALUES
	(5, 'REPORT_SMTP_FROMADDRESS', 'developer@musonisystem.com');

update m_report_mailing_job_configuration set name = 'REPORT_SMTP_SERVER' where name = 'GMAIL_SMTP_SERVER';
update m_report_mailing_job_configuration set name = 'REPORT_SMTP_PORT' where name = 'GMAIL_SMTP_PORT';
update m_report_mailing_job_configuration set name = 'REPORT_SMTP_USERNAME' where name = 'GMAIL_SMTP_USERNAME';
update m_report_mailing_job_configuration set name = 'REPORT_SMTP_PASSWORD' where name = 'GMAIL_SMTP_PASSWORD';


