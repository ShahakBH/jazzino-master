create table stg_player_activity_hourly(
PLAYER_ID DECIMAL(16,2) NOT NULL DISTKEY,
GAME varchar(255),
PLATFORM varchar(255),
ACTIVITY_TS timestamp not null DEFAULT SYSDATE SORTKEY
);

GRANT SELECT ON stg_player_activity_hourly TO GROUP READ_ONLY;
GRANT ALL ON stg_player_activity_hourly TO GROUP READ_WRITE;
GRANT ALL ON stg_player_activity_hourly TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('stg_player_activity_hourly', 'PLAYER_ID,ACTIVITY_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');
