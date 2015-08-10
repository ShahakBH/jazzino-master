CREATE TABLE aggregator_last_update (
id varchar(64) not null distkey sortkey PRIMARY KEY,
last_run_ts timestamp not null
);

GRANT SELECT ON aggregator_last_update TO GROUP READ_ONLY;
GRANT ALL ON aggregator_last_update TO GROUP READ_WRITE;
GRANT ALL ON aggregator_last_update TO GROUP SCHEMA_MANAGER;
