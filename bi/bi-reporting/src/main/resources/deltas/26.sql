CREATE TABLE aggregator_lock (
  id varchar(64) not null distkey sortkey,
  lock_client varchar(255) not null
);

GRANT SELECT ON aggregator_lock TO GROUP READ_ONLY;
GRANT ALL ON aggregator_lock TO GROUP READ_WRITE;
GRANT ALL ON aggregator_lock TO GROUP SCHEMA_MANAGER;
