create table adnet_registration_bands (
	registration_date TIMESTAMP not null ,
	registration_platform  varchar(128) not null,
	band varchar(128) not null,
	num_registrations bigint not null);

alter table adnet_registration_bands add primary key(registration_date,registration_platform, band);

GRANT SELECT ON adnet_registration_bands TO GROUP READ_ONLY;
GRANT ALL ON adnet_registration_bands TO GROUP READ_WRITE;
GRANT ALL ON adnet_registration_bands TO GROUP SCHEMA_MANAGER;
