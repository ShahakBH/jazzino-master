create table dmr_player_activity_and_purchases (
  player_id DECIMAL(16,2) NOT NULL DISTKEY,
	game varchar(32),
	platform varchar(32),
	activity_date timestamp not null,
	referrer varchar(255),
	registration_date timestamp not null,
	registration_platform varchar(32),
	num_registrations int ,
	player_purchase_daily_player_id DECIMAL(16,2) ,
	player_purchase_daily_registration_date timestamp DEFAULT SYSDATE ,
	purchase_platform varchar(32),
	purchase_date timestamp DEFAULT SYSDATE,
	total_amount_gbp decimal(32,4),
	num_purchases int,
	PRIMARY KEY (player_id,activity_date)
	) SORTKEY(player_id,activity_date);

GRANT SELECT ON dmr_player_activity_and_purchases TO GROUP READ_ONLY;
GRANT ALL ON dmr_player_activity_and_purchases TO GROUP READ_WRITE;
GRANT ALL ON dmr_player_activity_and_purchases TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('dmr_player_activity_and_purchases', 'player_id,activity_date','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');

