create table dmr_registrations (
	registration_date TIMESTAMP not null SORTKEY DISTKEY,
	registration_platform  varchar(20) not null,
	days_ago int not null,
	num_registrations int not null);

GRANT SELECT ON dmr_registrations TO GROUP READ_ONLY;
GRANT ALL ON dmr_registrations TO GROUP READ_WRITE;
GRANT ALL ON dmr_registrations TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('dmr_registrations', 'registration_date','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');
