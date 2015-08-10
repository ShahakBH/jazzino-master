create table stg_last_played_mv (
player_id DECIMAL(16,2) not null distkey sortkey PRIMARY KEY,
last_played_ts timestamp without time zone not null
);

GRANT SELECT ON stg_last_played_mv TO GROUP READ_ONLY;
GRANT ALL ON stg_last_played_mv TO GROUP READ_WRITE;
GRANT ALL ON stg_last_played_mv TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('stg_last_played_mv', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');
