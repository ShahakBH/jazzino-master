CREATE TABLE stg_promo_reward(
  player_id DECIMAL(16,2) NOT NULL DISTKEY,
	promo_id bigint not null,
  activity_ts timestamp not null SORTKEY,
  PRIMARY KEY (player_id,promo_id, activity_ts));

GRANT SELECT ON stg_promo_reward TO GROUP READ_ONLY;
GRANT ALL ON stg_promo_reward TO GROUP READ_WRITE;
GRANT ALL ON stg_promo_reward TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('stg_promo_reward', 'player_id, activity_date','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');
