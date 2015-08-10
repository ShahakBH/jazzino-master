
drop table maintenance;

CREATE TABLE maintenance (
  table_name VARCHAR(128) NOT NULL DISTKEY SORTKEY PRIMARY KEY, -- the name of the table to maintain
  key_columns TEXT NULL,                                        -- the key columns to analyse on the 'key' frequency, comma-separated
  vacuumed TIMESTAMP NOT NULL,                                      -- the last time the table was vacuumed
  vacuum_period_hours SMALLINT NOT NULL DEFAULT 24,             -- how often to vacuum the table
  table_analysed TIMESTAMP NOT NULL,                                -- the last time the table was analysed
  table_analyse_hours SMALLINT NOT NULL DEFAULT 168,            -- how often to analyse the table
  keys_analysed TIMESTAMP NOT NULL,                                 -- the last time the table keys were analysed
  keys_analyse_hours SMALLINT NOT NULL DEFAULT 24,              -- how often to analyse the table
  run_hour SMALLINT NOT NULL DEFAULT 10,                        -- hour to run maintenance at
  run_minute SMALLINT NOT NULL DEFAULT 0                        -- minute to run maintenance at
);

GRANT SELECT ON maintenance TO GROUP READ_ONLY;
GRANT ALL ON maintenance TO GROUP READ_WRITE;
GRANT ALL ON maintenance TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('account', 'ACCOUNT_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('account_session', 'ACCOUNT_ID,START_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('aggregator_last_update', 'ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('aggregator_lock', 'ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('audit_closed_game', 'AUDIT_TS,DBWRITE_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('audit_closed_game_player', 'TABLE_ID,AUDIT_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('audit_command', 'AUDIT_TS,PLAYER_ID,TABLE_ID,COMMAND_TYPE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('cashier_platform', 'CASHIER_NAME','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('changelog', 'CHANGE_NUMBER','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('currency_rates', 'CURRENCY_CODE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('dau_mau', 'DATE,INTERVAL','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('engagement_by_platform', 'PLAYED_TS,TRANSACTION_TYPE,PLAYED_TS,PLATFORM,GAME_VARIATION_TEMPLATE_NAME','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('external_transaction', 'ACCOUNT_ID,MESSAGE_TS,EXTERNAL_TRANSACTION_STATUS,CURRENCY_CODE,CASHIER_NAME','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('game_variation_template', 'GAME_VARIATION_TEMPLATE_ID,GAME_TYPE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('invitations', 'PLAYER_ID,CREATED_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('last_played_mv', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('leaderboard', 'LEADERBOARD_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('leaderboard_position', 'LEADERBOARD_ID,PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('lobby_user', 'PLAYER_ID,REG_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_activity_daily', 'PLAYER_ID,ACTIVITY_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_activity_hourly', 'PLAYER_ID,ACTIVITY_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_definition', 'PLAYER_ID,CREATED_TS,ACCOUNT_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_level', 'PLAYER_ID,GAME_TYPE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_mv', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('player_referrer', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_account', 'ACCOUNT_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_game_variation_template','GAME_VARIATION_TEMPLATE_ID,GAME_TYPE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_invitations', 'PLAYER_ID,CREATED_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_leaderboard', 'LEADERBOARD_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_lobby_user', 'PLAYER_ID,REG_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_player_definition', 'PLAYER_ID,CREATED_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_player_level', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_player_referrer', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('stg_tournament_variation_template', 'TOURNAMENT_VARIATION_TEMPLATE_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('table_definition', 'TABLE_ID,GAME_VARIATION_TEMPLATE_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('tournament', 'TOURNAMENT,TOURNAMENT_START_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('tournament_player', 'PLAYER_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('tournament_player_summary', 'PLAYER_ID,TOURNAMENT_ID','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('tournament_summary', 'TOURNAMENT_ID,TOURNAMENT_FINISHED_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('tournament_variation_template', 'TOURNAMENT_VARIATION_TEMPLATE_ID,GAME_TYPE','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11'),
  ('transaction_log', 'ACCOUNT_ID,TRANSACTION_TS','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');


