CREATE TABLE adnet_registrations_tmp_data(
  registration_date date SORTKEY DISTKEY,
  registration_adnet character varying(128),
  num_registrations bigint,
  PRIMARY KEY(registration_date, registration_adnet));


GRANT SELECT ON adnet_registrations_tmp_data TO GROUP READ_ONLY;
GRANT ALL ON adnet_registrations_tmp_data TO GROUP READ_WRITE;
GRANT ALL ON adnet_registrations_tmp_data TO GROUP SCHEMA_MANAGER;

--doesn't need maintaining as it's completely rebuilt every time.
