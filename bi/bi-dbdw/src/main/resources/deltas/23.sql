-- the events being dropped are no longer required and have been replaced by event extractAccountActivity. This event
-- calls extractAccountActivity().
--  Note that the following tables will no longer be updated:
--
-- rpt_distinct_players
-- rpt_distinct_players_weekly
-- rpt_distinct_players_monthly
-- rpt_distinct_users_by_game_source
-- rpt_distinct_users_by_game_source_weekly
-- rpt_distinct_users_by_game_source_monthly
--
-- these tables have been superceded by:
--
-- rpt_players_daily
-- rpt_players_weekly
-- rpt_players_monthly
-- rpt_users_daily
-- rpt_users_weekly
-- rpt_users_monthly

drop event if exists rptExtractDistinctPlayers#
drop event if exists rptExtractDistinctUsersByGameSource#
drop event if exists rptExtractDistinctUsersByGameSourceMonthly#
drop event if exists rptExtractDistinctUsersByGameSourceWeekly#
