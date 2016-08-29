ALTER TABLE `m_entity_datatable_check`
   DROP INDEX `unique_entity_check`, 
   ADD UNIQUE KEY `unique_entity_check` (`application_table_name`,`x_registered_table_id`,`status_enum`,`product_loan_id`);