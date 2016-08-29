ALTER TABLE m_entity_datatable_check ADD COLUMN product_loan_id bigint(10) NULL; 

ALTER TABLE m_entity_datatable_check ADD FOREIGN KEY (product_loan_id) REFERENCES m_product_loan(id);