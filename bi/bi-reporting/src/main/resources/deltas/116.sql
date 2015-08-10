CREATE TABLE adnet_registrations(
  registration_date date SORTKEY DISTKEY,
  registration_adnet character varying(128),
  days_ago integer,
  num_registrations bigint,
  PRIMARY KEY(registration_date, registration_adnet, days_ago));


GRANT SELECT ON adnet_registrations TO GROUP READ_ONLY;
GRANT ALL ON adnet_registrations TO GROUP READ_WRITE;
GRANT ALL ON adnet_registrations TO GROUP SCHEMA_MANAGER;

--doesn't need maintaining as it's completely rebuilt every time.
