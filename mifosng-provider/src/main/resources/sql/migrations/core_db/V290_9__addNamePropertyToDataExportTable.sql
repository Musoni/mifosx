ALTER TABLE `m_data_export` ADD `name` VARCHAR(100) NOT NULL AFTER `id`;

update `m_data_export` set name = concat(base_entity_name, ' data export ', id);

alter table `m_data_export` add constraint unique_name unique (name);