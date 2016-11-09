create table if not exists `m_data_export` (
id bigint(20) primary key auto_increment,
base_entity_name varchar(20) not null,
user_request_map text,
data_sql text not null,
is_deleted tinyint(1) default 0 not null,
filename varchar(20),
file_download_count int not null default 0,
createdby_id bigint not null,
created_date datetime not null,
lastmodifiedby_id bigint,
lastmodified_date datetime,
foreign key (createdby_id) references m_appuser(id),
foreign key (lastmodifiedby_id) references m_appuser(id));

INSERT INTO `m_permission`
(`grouping`,`code`, `entity_name`, `action_name`, `can_maker_checker`) VALUES
('dataexport', 'CREATE_DATAEXPORT', 'DATAEXPORT', 'CREATE', 0),
('dataexport', 'READ_DATAEXPORT', 'DATAEXPORT', 'READ', 0),
('dataexport', 'DELETE_DATAEXPORT', 'DATAEXPORT', 'DELETE', 0),
('dataexport', 'UPDATE_DATAEXPORT', 'DATAEXPORT', 'UPDATE', 0);