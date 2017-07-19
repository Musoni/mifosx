CREATE TABLE IF NOT EXISTS `m_savings_product_interest_rate_chart` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `savings_product_id` bigint(20) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `from_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `annual_interest_rate` decimal(19,6) DEFAULT NULL,
  `apply_to_existing_savings_account` TINYINT(1) DEFAULT 0 NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`savings_product_id`) REFERENCES `m_savings_product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
