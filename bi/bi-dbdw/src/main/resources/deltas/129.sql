drop view if exists rpt_payout_by_game_variation_daily_summary#

drop view if exists rpt_payout_daily_summary#

drop event if exists evt_calc_agg_daily_payout_by_game_variation#

drop event if exists evt_calc_agg_transaction_log_by_date_transaction_type_nb#

drop event if exists evt_calc_agg_daily_payout_by_game_variation#

drop event if exists evt_calc_agg_transaction_log_by_date_transaction_type_nb#

drop procedure if exists tmp_leaderboard_migration_addcol#

drop procedure if exists mm_tournaments_participation#
