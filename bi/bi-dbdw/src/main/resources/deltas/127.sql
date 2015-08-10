drop procedure if exists init_agg_transaction_log_by_date_transaction_type#

drop procedure if exists calc_agg_transaction_log_by_date_transaction_type_nb#

drop procedure if exists calc_agg_transaction_log_by_date_transaction_type#

drop procedure if exists calc_agg_daily_payout_by_game_variation#

drop table if exists AGG_TRANSACTION_LOG_BY_DATE_TRANSACTION_TYPE#

drop table if exists AGG_DAILY_PAYOUT_BY_GAME_VARIATION#

delete from $CONFIGURATION
where name = 'agg_transaction_log_by_date_transaction_type_batch_size'#

delete from $STATUS
where name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id'#
