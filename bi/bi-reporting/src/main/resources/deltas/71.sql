CREATE TABLE maintenance (
  table_name VARCHAR(128) NOT NULL DISTKEY SORTKEY PRIMARY KEY, -- the name of the table to maintain
  key_columns TEXT NULL,                                        -- the key columns to analyse on the 'key' frequency, comma-separated
  vacuumed TIMESTAMP NULL,                                      -- the last time the table was vacuumed
  vacuum_period_hours SMALLINT NOT NULL DEFAULT 24,             -- how often to vacuum the table
  table_analysed TIMESTAMP NULL,                                -- the last time the table was analysed
  table_analyse_hours SMALLINT NOT NULL DEFAULT 168,            -- how often to analyse the table
  keys_analysed TIMESTAMP NULL,                                 -- the last time the table keys were analysed
  keys_analyse_hours SMALLINT NOT NULL DEFAULT 24,              -- how often to analyse the table
  run_hour SMALLINT NOT NULL DEFAULT 10,                        -- hour to run maintenance at
  run_minute SMALLINT NOT NULL DEFAULT 0                        -- minute to run maintenance at
);

GRANT SELECT ON maintenance TO GROUP READ_ONLY;
GRANT ALL ON maintenance TO GROUP READ_WRITE;
GRANT ALL ON maintenance TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns) VALUES
  ('account', 'ACCOUNT_ID'),
  ('account_session', 'ACCOUNT_ID,START_TS'),
  ('aggregator_last_update', 'ID'),
  ('aggregator_lock', 'ID'),
  ('audit_closed_game', 'AUDIT_TS,DBWRITE_TS'),
  ('audit_closed_game_player', 'TABLE_ID,AUDIT_TS'),
  ('audit_command', 'AUDIT_TS,PLAYER_ID,TABLE_ID,COMMAND_TYPE'),
  ('cashier_platform', 'CASHIER_NAME'),
  ('changelog', 'CHANGE_NUMBER'),
  ('currency_rates', 'CURRENCY_CODE'),
  ('dau_mau', 'DATE,INTERVAL'),
  ('engagement_by_platform', 'PLAYED_TS,TRANSACTION_TYPE,PLAYED_TS,PLATFORM,GAME_VARIATION_TEMPLATE_NAME'),
  ('external_transaction', 'ACCOUNT_ID,MESSAGE_TS,EXTERNAL_TRANSACTION_STATUS,CURRENCY_CODE,CASHIER_NAME'),
  ('game_variation_template', 'GAME_VARIATION_TEMPLATE_ID,GAME_TYPE'),
  ('invitations', 'PLAYER_ID,CREATED_TS'),
  ('last_played_mv', 'PLAYER_ID'),
  ('leaderboard', 'LEADERBOARD_ID'),
  ('leaderboard_position', 'LEADERBOARD_ID,PLAYER_ID'),
  ('lobby_user', 'PLAYER_ID,REG_TS'),
  ('player_activity_daily', 'PLAYER_ID,ACTIVITY_TS'),
  ('player_activity_hourly', 'PLAYER_ID,ACTIVITY_TS'),
  ('player_definition', 'PLAYER_ID,CREATED_TS,ACCOUNT_ID'),
  ('player_level', 'PLAYER_ID,GAME_TYPE'),
  ('player_mv', 'PLAYER_ID'),
  ('player_referrer', 'PLAYER_ID'),
  ('stg_account', 'ACCOUNT_ID'),
  ('stg_game_variation_template','GAME_VARIATION_TEMPLATE_ID,GAME_TYPE'),
  ('stg_invitations', 'PLAYER_ID,CREATED_TS'),
  ('stg_leaderboard', 'LEADERBOARD_ID'),
  ('stg_lobby_user', 'PLAYER_ID,REG_TS'),
  ('stg_player_definition', 'PLAYER_ID,CREATED_TS'),
  ('stg_player_level', 'PLAYER_ID'),
  ('stg_player_referrer', 'PLAYER_ID'),
  ('stg_tournament_variation_template', 'TOURNAMENT_VARIATION_TEMPLATE_ID'),
  ('table_definition', 'TABLE_ID,GAME_VARIATION_TEMPLATE_ID'),
  ('tournament', 'TOURNAMENT,TOURNAMENT_START_TS'),
  ('tournament_player', 'PLAYER_ID'),
  ('tournament_player_summary', 'PLAYER_ID,TOURNAMENT_ID'),
  ('tournament_summary', 'TOURNAMENT_ID,TOURNAMENT_FINISHED_TS'),
  ('tournament_variation_template', 'TOURNAMENT_VARIATION_TEMPLATE_ID,GAME_TYPE'),
  ('transaction_log', 'ACCOUNT_ID,TRANSACTION_TS');
