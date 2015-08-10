
/********************************************************************************
 **      UNSUPPORTED VIEWS                                                     **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`INSIDERS_TODAY`#
DROP VIEW IF EXISTS `strataproddw`.`INSIDERS_YESTERDAY`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYERS_SINCE_M2`#
DROP VIEW IF EXISTS `strataproddw`.`GAME_STATS_TODAY`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYERS_TODAY`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_GAMES_10_MINS`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_GAMES_1_DAY`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_GAMES_1_HOUR`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_GAMES_BY_DATE`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_SESSIONS_1_DAY`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER_SESSIONS_1_HOUR`#
DROP VIEW IF EXISTS `strataproddw`.`TOURNAMENTS_YESTERDAY`#
DROP VIEW IF EXISTS `strataproddw`.`WEEK_COMPARISON`#




/********************************************************************************
 **      VIEW: GAMES_YESTERDAY                                                 **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`GAMES_YESTERDAY`#

CREATE VIEW `GAMES_YESTERDAY`
AS select
   count(distinct `a`.`TABLE_ID`,
   `a`.`GAME_ID`) AS `games`,
   `t`.`GAME_TYPE` AS `game_type`
from (`strataproddw`.`AUDIT_CLOSED_GAME_PLAYER` `a` join `strataproddw`.`TABLE_INFO` `t` on((`a`.`TABLE_ID` = `t`.`TABLE_ID`)))
where ((`a`.`AUDIT_TS` >= (curdate() - interval 1 day)) and (`a`.`AUDIT_TS` < curdate())) group by `t`.`GAME_TYPE`
#



/********************************************************************************
 **      VIEW: PLAYER_INFO                                                     **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`PLAYER_INFO`#

CREATE VIEW `PLAYER_INFO`
AS select
   `p`.`PLAYER_ID` AS `PLAYER_ID`,
   `pi`.`REGISTRATION_PLATFORM` AS `REGISTRATION_PLATFORM`,
   `pi`.`REGISTRATION_GAME` AS `REGISTRATION_GAME`,
   `pi`.`FIRST_PURCHASE` AS `FIRST_PURCHASE`
from (`strataproddw`.`PLAYER_ACCOUNT_INFO` `pi` left join `strataproddw`.`PLAYER` `p` on((`p`.`ACCOUNT_ID` = `pi`.`ACCOUNT_ID`)))
#




/********************************************************************************
 **      VIEW: PLAYER_PURCHASES                                                **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`PLAYER_PURCHASES`#

CREATE VIEW `PLAYER_PURCHASES`
AS select
   `P`.`PLAYER_ID` AS `PLAYER_ID`,
   `PP`.`USER_ID` AS `USER_ID`,
   `ET`.`AUTO_ID` AS `AUTO_ID`,
   `ET`.`MESSAGE_TIMESTAMP` AS `MESSAGE_TIMESTAMP`,
   ((`ET`.`CURRENCY_CODE` = 'USD') * `ET`.`AMOUNT`) AS `USD`,
   ((`ET`.`CURRENCY_CODE` = 'EUR') * `ET`.`AMOUNT`) AS `EUR`,
   ((`ET`.`CURRENCY_CODE` = 'GBP') * `ET`.`AMOUNT`) AS `GBP`
from (
  `strataproddw`.`EXTERNAL_TRANSACTION` `ET`
  join `strataproddw`.`PLAYER` `P`
  join `strataproddw`.`LOBBY_USER` `PP`
)
where (
  (`ET`.`ACCOUNT_ID` = `P`.`ACCOUNT_ID`)
  and (`PP`.`PLAYER_ID` = `P`.`PLAYER_ID`)
  and (`ET`.`EXTERNAL_TRANSACTION_STATUS` = 'SUCCESS')
)
#




/********************************************************************************
 **      VIEW: UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY                             **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY`#

CREATE VIEW `UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY`
AS select
   `TT`.`GAME_TYPE` AS `GAME_TYPE`, count(distinct `TP`.`PLAYER_ID`) AS `UNIQUE_PLAYERS`, count(distinct `T`.`TOURNAMENT_ID`) AS `TOURNAMENTS`
from ((((`strataproddw`.`PLAYER` `P` join `strataproddw`.`TOURNAMENT_PLAYER` `TP`) join `strataproddw`.`TOURNAMENT` `T`) join `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE` `TT`) join `strataproddw`.`TOURNAMENT_SUMMARY` `TS`)
where (isnull(`P`.`IS_INSIDER`) and (`TP`.`PLAYER_ID` = `P`.`PLAYER_ID`) and (`TP`.`TOURNAMENT_ID` = `T`.`TOURNAMENT_ID`) and (`T`.`TOURNAMENT_VARIATION_TEMPLATE_ID` = `TT`.`TOURNAMENT_VARIATION_TEMPLATE_ID`) and (`TS`.`TOURNAMENT_ID` = `T`.`TOURNAMENT_ID`) and (`TS`.`TOURNAMENT_FINISHED_TS` > cast((now() - interval 1 day) as date)) and (`TS`.`TOURNAMENT_FINISHED_TS` < cast(now() as date))) group by `TT`.`GAME_TYPE`
#




/********************************************************************************
 **      VIEW: UNIQUE_PLAYERS_YESTERDAY                                        **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`UNIQUE_PLAYERS_YESTERDAY`#

CREATE VIEW `UNIQUE_PLAYERS_YESTERDAY`
AS select
   distinct `a`.`PLAYER_ID` AS `player_id`,
   `aid`.`PLATFORM` AS `partner_id`
from ((`strataproddw`.`rpt_account_activity` `a` left join `strataproddw`.`PLAYER` `pp` on((`pp`.`PLAYER_ID` = `a`.`PLAYER_ID`))) left join `strataproddw`.`rpt_activity_by_account_id` `aid` on(((`pp`.`ACCOUNT_ID` = `aid`.`ACCOUNT_ID`) and (`aid`.`PLATFORM` <> '') and ((`aid`.`AUDIT_DATE` = (curdate() - interval 1 day)) or (`aid`.`AUDIT_DATE` = (curdate() - interval 2 day)) or (`aid`.`AUDIT_DATE` = curdate())))))
where ((`a`.`AUDIT_DATE` = (curdate() - interval 1 day)) and (`a`.`GAME_TYPE` <> ''))
#




/********************************************************************************
 **      VIEW: MM_CHIPS                                                        **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_chips`#

CREATE VIEW `mm_chips`
AS select
   `l`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `l`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `l`.`USER_ID` AS `USER_ID`,
   `a`.`BALANCE` AS `BALANCE`
from ((`strataproddw`.`ACCOUNT` `a` join `strataproddw`.`PLAYER` `p` on((`a`.`ACCOUNT_ID` = `p`.`ACCOUNT_ID`))) join `strataproddw`.`LOBBY_USER` `l` on((`l`.`PLAYER_ID` = `p`.`PLAYER_ID`)))
#




/********************************************************************************
 **      VIEW: MM_EXTERNAL_TRANSACTIONS                                        **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_external_transactions`#

CREATE VIEW `mm_external_transactions`
AS select
   `ET`.`AUTO_ID` AS `AUTO_ID`,
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `ET`.`GAME_TYPE` AS `GAME_TYPE`,
   `ET`.`AMOUNT_CHIPS` AS `AMOUNT_CHIPS`,
   `ET`.`AMOUNT` AS `AMOUNT`,
   `ET`.`CURRENCY_CODE` AS `CURRENCY_CODE`,
   `ET`.`TRANSACTION_TYPE` AS `TRANSACTION_TYPE`,
   `ET`.`MESSAGE_TIMESTAMP` AS `TIMESTAMP`,
   `ET`.`EXTERNAL_TRANSACTION_STATUS` AS `EXTERNAL_TRANSACTION_STATUS`
from ((`strataproddw`.`EXTERNAL_TRANSACTION` `ET` join `strataproddw`.`PLAYER` `P`) left join `strataproddw`.`LOBBY_USER` `lu` on((`P`.`PLAYER_ID` = `lu`.`PLAYER_ID`)))
where ((`ET`.`EXTERNAL_TRANSACTION_STATUS` = 'SUCCESS') and (`P`.`ACCOUNT_ID` = `ET`.`ACCOUNT_ID`))
#




/********************************************************************************
 **      VIEW: MM_INITIAL_GAMES                                                **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_initial_games`#

CREATE VIEW `mm_initial_games`
AS select
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `lp`.`GAME_TYPE` AS `GAME_TYPE`,
   `lp`.`LAST_PLAYED` AS `LAST_TS`
from ((`strataproddw`.`LAST_PLAYED` `lp` join `strataproddw`.`PLAYER` `p`) join `strataproddw`.`LOBBY_USER` `lu`)
where ((`lp`.`PLAYER_ID` = `p`.`PLAYER_ID`) and (`p`.`PLAYER_ID` = `lu`.`PLAYER_ID`))
#




/********************************************************************************
 **      VIEW: MM_LEADERBOARD_STATE                                            **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_leaderboard_state`#

CREATE VIEW `mm_leaderboard_state`
AS select
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `lastBoard`.`GAME_TYPE` AS `GAME_TYPE`,
   `lbp`.`LEADERBOARD_POSITION` AS `BOARD_GRADE`
from (((`strataproddw`.`mm_last_leaderboard` `lastBoard` join `strataproddw`.`LEADERBOARD_POSITION` `lbp`) join `strataproddw`.`PLAYER` `p`) join `strataproddw`.`LOBBY_USER` `lu`)
where ((`lastBoard`.`LEADERBOARD_ID` = `lbp`.`LEADERBOARD_ID`) and (`lbp`.`PLAYER_ID` = `p`.`PLAYER_ID`) and (`p`.`PLAYER_ID` = `lu`.`PLAYER_ID`))
#




/********************************************************************************
 **      VIEW: MM_LOGINS                                                       **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_logins`#

CREATE VIEW `mm_logins`
AS select
   `l`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `l`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `l`.`USER_ID` AS `USER_ID`,
   `s`.`IP_ADDRESS` AS `IP_ADDRESS`,
   `s`.`REFERER` AS `SOURCE`,
   `s`.`TSSTARTED` AS `LOGIN_TIME`
from ((`strataproddw`.`ACCOUNT_SESSION` `s` join `strataproddw`.`PLAYER` `p` on((`s`.`ACCOUNT_ID` = `p`.`ACCOUNT_ID`))) join `strataproddw`.`LOBBY_USER` `l` on((`l`.`PLAYER_ID` = `p`.`PLAYER_ID`))) order by `s`.`TSSTARTED`
#




/********************************************************************************
 **      VIEW: MM_TOTAL_INVITES                                                **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_total_invites`
#

CREATE VIEW `mm_total_invites`
AS select
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `i`.`STATUS` AS `STATUS`,unix_timestamp(`i`.`CREATED_TS`) AS `CREATED_TS`,unix_timestamp(`i`.`UPDATED_TS`) AS `UPDATED_TS`
from ((`strataproddw`.`INVITATIONS` `i` join `strataproddw`.`PLAYER` `p`) join `strataproddw`.`LOBBY_USER` `lu`)
where ((`i`.`PLAYER_ID` = `p`.`PLAYER_ID`) and (`p`.`PLAYER_ID` = `lu`.`PLAYER_ID`)) order by `i`.`PLAYER_ID`
#




/********************************************************************************
 **      VIEW: MM_TOURNAMENTS_PARTICIPATION                                    **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_tournaments_participation`
#

CREATE VIEW `mm_tournaments_participation`
AS select
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `tvt`.`GAME_TYPE` AS `GAME_TYPE`,unix_timestamp(`ts`.`TOURNAMENT_FINISHED_TS`) AS `LAST_TIMESTAMP`
from (((((`strataproddw`.`TOURNAMENT_SUMMARY` `ts` join `strataproddw`.`TOURNAMENT_PLAYER` `tp`) join `strataproddw`.`TOURNAMENT` `t`) join `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE` `tvt`) join `strataproddw`.`PLAYER` `p`) join `strataproddw`.`LOBBY_USER` `lu`)
where ((`ts`.`TOURNAMENT_ID` = `tp`.`TOURNAMENT_ID`) and (`tp`.`TOURNAMENT_ID` = `t`.`TOURNAMENT_ID`) and (`t`.`TOURNAMENT_VARIATION_TEMPLATE_ID` = `tvt`.`TOURNAMENT_VARIATION_TEMPLATE_ID`) and (`tp`.`PLAYER_ID` = `p`.`PLAYER_ID`) and (`p`.`PLAYER_ID` = `lu`.`PLAYER_ID`))
#




/********************************************************************************
 **      VIEW: MM_LAST_LEVELS                                                  **
 ********************************************************************************/

DROP VIEW IF EXISTS mm_last_levels#
CREATE VIEW mm_last_levels AS
SELECT
  lu.RPX_PROVIDER AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,p.LEVEL AS LEVEL,lp.LAST_PLAYED AS LAST_PLAYED
FROM
  mm_last_played lp
  JOIN strataproddw.PLAYER p
  JOIN strataproddw.LOBBY_USER lu
WHERE
  lp.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID AND NOT p.LEVEL IS NULL
#




/********************************************************************************
 **      VIEW: RPT_PAYOUT_BY_GAME_VARIATION_DAILY_SUMMARY                      **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`rpt_payout_by_game_variation_daily_summary`
#

CREATE VIEW `rpt_payout_by_game_variation_daily_summary`
AS select
   `s`.`date` AS `date`,
   `gvt`.`NAME` AS `Game Variation`,(`s`.`total_return` / `s`.`total_stake`) AS `Payout`,
   `s`.`total_stake` AS `Total Stake`,
   `s`.`total_num_stakes` AS `Total Number of Stakes`,
   `s`.`total_return` AS `Total Return`,
   `s`.`total_num_returns` AS `Total Number of Returns`,(`s`.`total_num_returns` / `s`.`total_num_stakes`) AS `Win Ratio`,(`s`.`total_num_stakes` + `s`.`total_num_returns`) AS `Total Transactions`
from (`strataproddw`.`AGG_DAILY_PAYOUT_BY_GAME_VARIATION` `s` join `strataproddw`.`GAME_VARIATION_TEMPLATE` `gvt`)
where (`s`.`game_variation_template_id` = `gvt`.`GAME_VARIATION_TEMPLATE_ID`)
#




/********************************************************************************
 **      VIEW: RPT_PAYOUT_DAILY_SUMMARY                                        **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`rpt_payout_daily_summary`#

CREATE VIEW `rpt_payout_daily_summary`
AS select
   `s`.`date` AS `date`,(sum(`s`.`total_return`) / sum(`s`.`total_stake`)) AS `Payout`,sum(`s`.`total_stake`) AS `Total Stake`,sum(`s`.`total_num_stakes`) AS `Total Number of Stakes`,sum(`s`.`total_return`) AS `Total Return`,sum(`s`.`total_num_returns`) AS `Total Number of Returns`,(sum(`s`.`total_num_returns`) / sum(`s`.`total_num_stakes`)) AS `Win Ratio`,(sum(`s`.`total_num_stakes`) + sum(`s`.`total_num_returns`)) AS `Total Transactions`
from (`strataproddw`.`AGG_DAILY_PAYOUT_BY_GAME_VARIATION` `s` join `strataproddw`.`GAME_VARIATION_TEMPLATE` `gvt`)
where (`s`.`game_variation_template_id` = `gvt`.`GAME_VARIATION_TEMPLATE_ID`) group by `s`.`date`
#




/********************************************************************************
 **      VIEW: RPT_PAYOUT_DAILY_SUMMARY                                        **
 ********************************************************************************/

DROP VIEW IF EXISTS rpt_player_sources#

CREATE VIEW rpt_player_sources AS
SELECT
  p.USER_ID AS USER_ID,
  p.PLAYER_ID AS PLAYER_ID,
  CASE WHEN at.AD_CODE IS NULL THEN (CASE WHEN REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE at.AD_CODE END AS SOURCE,
  p.ACCOUNT_ID AS ACCOUNT_ID,
  p.tscreated AS TSCREATED
FROM
  strataproddw.PLAYER p
  JOIN strataproddw.LOBBY_USER lu ON p.USER_ID=lu.USER_ID
  LEFT JOIN AD_TRACKING at ON p.USER_ID=at.USER_ID
ORDER BY p.USER_ID DESC#




/********************************************************************************
 **      LEADERBOARD: Add END_TS to LEADERBOARD table                          **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS `strataproddw`.`tmp_leaderboard_migration_addcol`
#

CREATE PROCEDURE `strataproddw`.`tmp_leaderboard_migration_addcol`()
BEGIN
IF NOT EXISTS(
	SELECT * FROM information_schema.COLUMNS
	WHERE COLUMN_NAME='END_TS' AND TABLE_NAME='LEADERBOARD' AND TABLE_SCHEMA='strataproddw'
	)
	THEN
      ALTER TABLE `strataproddw`.`LEADERBOARD`
      ADD END_TS TIMESTAMP NULL DEFAULT NULL;
END IF;
END;
#

CALL `strataproddw`.`tmp_leaderboard_migration_addcol`
#


DROP PROCEDURE IF EXISTS `strataproddw`.`tmp_leaderboard_migration`
#

CREATE PROCEDURE `strataproddw`.`tmp_leaderboard_migration`()
BEGIN
  DECLARE _leaderboard_id INT;
  DECLARE _end_ts timestamp;
  DECLARE done INT DEFAULT 0;

  DECLARE c CURSOR FOR SELECT `LEADERBOARD_ID`, `END_TS` FROM strataprod.LEADERBOARD;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN c;

  read_loop: LOOP
    FETCH c INTO _leaderboard_id, _end_ts;

    IF done THEN
      LEAVE read_loop;
    END IF;

    UPDATE `strataproddw`.`LEADERBOARD`
    SET `end_ts` = _end_ts
    WHERE `leaderboard_id` = _leaderboard_id;
  END LOOP;

  CLOSE c;
end
#

call `strataproddw`.`tmp_leaderboard_migration`()
#

DROP PROCEDURE `strataproddw`.`tmp_leaderboard_migration`
#




/********************************************************************************
 **      VIEW: MM_LAST_LEADERBOARD                                             **
 ********************************************************************************/

DROP VIEW IF EXISTS `strataproddw`.`mm_last_leaderboard`#

CREATE VIEW `mm_last_leaderboard`
AS select
   `strataproddw`.`LEADERBOARD`.`LEADERBOARD_ID` AS `LEADERBOARD_ID`,
   `strataproddw`.`LEADERBOARD`.`GAME_TYPE` AS `GAME_TYPE`
from `strataproddw`.`LEADERBOARD`
where (`strataproddw`.`LEADERBOARD`.`END_TS` is not null)
order by `strataproddw`.`LEADERBOARD`.`END_TS` desc
#
