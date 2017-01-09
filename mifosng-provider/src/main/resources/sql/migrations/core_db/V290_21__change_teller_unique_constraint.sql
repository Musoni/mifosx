
ALTER TABLE m_tellers DROP INDEX  m_tellers_name_unq,
ADD CONSTRAINT m_teller_unique_name UNIQUE (`name`, `office_id`);