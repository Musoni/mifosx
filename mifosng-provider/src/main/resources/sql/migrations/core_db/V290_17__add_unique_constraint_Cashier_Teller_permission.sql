ALTER TABLE m_cashiers ADD CONSTRAINT `ux_cashiers_staff_teller` UNIQUE (teller_id,staff_id);