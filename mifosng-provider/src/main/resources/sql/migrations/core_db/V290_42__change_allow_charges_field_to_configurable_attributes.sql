ALTER TABLE m_product_loan DROP COLUMN allow_additional_charges;

ALTER TABLE m_product_loan_configurable_attributes ADD COLUMN allow_additional_charges TINYINT(1) DEFAULT '1' NOT NULL;