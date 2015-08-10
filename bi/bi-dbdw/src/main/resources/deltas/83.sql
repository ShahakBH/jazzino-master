create event if not exists evt_calc_agg_daily_payout_by_game_variation
on schedule every 1 day
starts curdate() + interval 1 day + interval 7 hour
comment 'Calculate daily payout by game variation.'
do call calc_agg_daily_payout_by_game_variation(curdate() - interval 1 day)#

create event if not exists evtFillDailyMailStats
on schedule every 1 day
starts curdate() + interval 1 day + interval 7 hour + interval 20 minute
DO call fillDailyMailStats()#

alter event evt_calc_agg_daily_payout_by_game_variation
on schedule every 1 day
starts curdate() + interval 1 day + interval 7 hour#

alter event evtFillDailyMailStats
on schedule every 1 day
starts curdate() + interval 1 day + interval 7 hour + interval 20 minute#
