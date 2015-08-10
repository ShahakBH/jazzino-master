-- WEB-4513 - standardise indexing; remove foreign keys

create index account_session_account_id_idx on account_session (account_id);
create index account_session_start_ts_idx on account_session (start_ts);

alter table invitations drop constraint invitations_player_id_fkey;
create index invitations_player_id_idx on invitations (player_id);

alter table transaction_log drop constraint transaction_log_session_id_fkey;
create index transaction_log_session_id_idx on transaction_log (session_id);

alter table audit_closed_game_player drop constraint audit_closed_game_player_player_id_fkey;
create index audit_closed_game_player_player_id_idx on audit_closed_game_player (player_id);

alter table audit_command drop constraint audit_command_player_id_fkey;
create index audit_command_player_id_idx on audit_command (player_id);

alter table last_played_mv drop constraint last_played_mv_player_id_fkey;
create index last_played_mv_player_id_idx on last_played_mv (player_id);

alter table leaderboard_position drop constraint leaderboard_position_player_id_fkey;
create index leaderboard_position_player_id_idx on leaderboard_position (player_id);

drop index if exists player_level_player_id_idx;
alter table player_level drop constraint if exists player_level_player_id_fkey;
alter table player_level drop constraint if exists player_level_player_id_fkey1;
create index player_level_player_id_idx on player_level (player_id);

alter table stg_invitations drop constraint stg_invitations_player_id_fkey;

alter table tournament_player drop constraint tournament_player_player_id_fkey;
create index tournament_player_player_id_idx on tournament_player (player_id);

alter table tournament_player_summary drop constraint tournament_player_summary_player_id_fkey;
create index tournament_player_summary_player_id_idx on tournament_player_summary (player_id);

create index audit_closed_game_game_id_idx on audit_closed_game(game_id);
create index audit_closed_game_table_id_idx on audit_closed_game(table_id);
create index audit_closed_game_audit_ts_idx on audit_closed_game(audit_ts);

create index audit_closed_game_player_game_id_idx on audit_closed_game_player(game_id);
create index audit_closed_game_player_table_id_idx on audit_closed_game_player(table_id);
create index audit_closed_game_player_audit_ts_idx on audit_closed_game_player(audit_ts);

create index audit_command_table_id_idx on audit_command(table_id);
create index audit_command_game_id_idx on audit_command(game_id);
create index audit_command_audit_ts_idx on audit_command(audit_ts);

create index external_transaction_account_id_idx on external_transaction(account_id);
create index external_transaction_player_id_idx on external_transaction(player_id);
create index external_transaction_message_ts_idx on external_transaction(message_ts);
create index external_transaction_external_transaction_status_idx on external_transaction(external_transaction_status);
create index external_transaction_external_transaction_id_idx on external_transaction(external_transaction_id);
create index external_transaction_external_currency_code_idx on external_transaction(currency_code);
create index external_transaction_external_cashier_name_idx on external_transaction(cashier_name);

create index transaction_log_account_id_idx on transaction_log(account_id);
create index transaction_log_table_id_idx on transaction_log(table_id);
create index transaction_log_game_id_idx on transaction_log(game_id);
create index transaction_log_transaction_ts_idx on transaction_log(transaction_ts);

alter table aggregator_lock add primary key (id);

create index campaign_notification_audit_campaign_run_id_idx on campaign_notification_audit (campaign_run_id);
create index campaign_notification_audit_player_id_idx on campaign_notification_audit (player_id);
create index campaign_notification_audit_run_ts_date_idx on campaign_notification_audit (date_trunc('day', run_ts));
-- dropping primary key constraint because we send more than one message to a player if they have multiple devices.
-- this should be added to a delta after migration
alter table campaign_notification_audit drop constraint IF EXISTS campaign_notification_audit_pkey;

create index campaign_run_audit_campaign_id_idx on campaign_run_audit (campaign_id);
create index campaign_run_audit_run_id_idx on campaign_run_audit (run_id);
create index campaign_run_audit_run_ts_idx on campaign_run_audit (run_ts);

create index client_log_player_id_idx on client_log (player_id);
create index client_log_table_id_idx on client_log (table_id);
create index client_log_error_code_idx on client_log (error_code);
create index client_log_log_ts_idx on client_log (log_ts);

create index dau_mau_date_idx on dau_mau (date);

create index dmr_player_activity_and_purchases_player_id_idx on dmr_player_activity_and_purchases (player_id);
-- remove index that doesn't follow the convention
drop index if exists dmr_player_activity_and_purchases_ppd_player_id_idx;
create index dmr_player_activity_and_purchases_player_purchase_daily_player_ on dmr_player_activity_and_purchases (player_purchase_daily_player_id);
create index dmr_player_activity_and_purchases_activity_date_idx on dmr_player_activity_and_purchases (activity_date);

create index dmr_registrations_registration_date_idx on dmr_registrations (registration_date);

create index leaderboard_position_leaderboard_id_idx on leaderboard_position (leaderboard_id);

create index player_activity_daily_player_id_idx on player_activity_daily (player_id);
create index player_activity_daily_activity_ts_idx on player_activity_daily (activity_ts);

create index player_activity_hourly_activity_ts_idx on player_activity_hourly (activity_ts);
create index player_activity_hourly_player_id_idx on player_activity_hourly (player_id);

alter table scheduled_aggregators add primary key (aggregator);

create index table_definition_game_variation_template_id_idx on table_definition (game_variation_template_id);

alter table dmr_player_activity_and_purchases drop constraint dmr_player_activity_and_purchases_pkey;
create unique index dmr_player_activity_and_purchases_pkey on dmr_player_activity_and_purchases (player_id,activity_date,platform);
alter table dmr_player_activity_and_purchases add primary key using index dmr_player_activity_and_purchases_pkey;

alter table engagement_by_platform drop constraint engagement_by_platform_pkey;
create unique index engagement_by_platform_pkey on engagement_by_platform (played_ts,platform,game_variation_template_name,transaction_type);
alter table engagement_by_platform add primary key using index engagement_by_platform_pkey;
