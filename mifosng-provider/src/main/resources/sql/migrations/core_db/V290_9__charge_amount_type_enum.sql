ALTER TABLE `m_charge`
ADD COLUMN `allow_override` TINYINT(1) NULL DEFAULT '0' AFTER `is_active`;
