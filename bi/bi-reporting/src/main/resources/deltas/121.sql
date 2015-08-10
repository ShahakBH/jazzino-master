CREATE TABLE adnet_dmr_days_tmp(
  registration_date date
);

GRANT SELECT ON adnet_dmr_days_tmp TO GROUP READ_ONLY;
GRANT ALL ON adnet_dmr_days_tmp TO GROUP READ_WRITE;
GRANT ALL ON adnet_dmr_days_tmp TO GROUP SCHEMA_MANAGER;

--doesn't need maintaining as it's completely rebuilt every time.
