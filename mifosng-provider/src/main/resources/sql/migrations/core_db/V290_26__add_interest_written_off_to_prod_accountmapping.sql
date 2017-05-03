INSERT INTO `acc_product_mapping` (`gl_account_id`,`product_id`,`product_type`,`payment_type`,`charge_id`,`financial_account_type`)
select mapping.gl_account_id, mapping.product_id, mapping.product_type,null,null, 14
from acc_product_mapping mapping
inner join m_product_loan m on m.id = mapping.product_id and (m.accounting_type = 3 or m.accounting_type =4)
where mapping.financial_account_type = 6 and mapping.product_type=1;