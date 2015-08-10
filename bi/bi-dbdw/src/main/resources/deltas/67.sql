-- Forcing reapply of some procedures

CREATE TABLE IF NOT EXISTS rpt_activity_by_account_id (
  ACCOUNT_ID bigint(20) not null,
  AUDIT_DATE date not null,
  GAME_TYPE varchar(255),
  primary key (ACCOUNT_ID, AUDIT_DATE, GAME_TYPE),
  key IDX_AUDIT_DATE (AUDIT_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#

ALTER TABLE rpt_activity_by_account_id 
ADD COLUMN PLATFORM VARCHAR(64) NOT NULL DEFAULT '' AFTER GAME_TYPE, 
CHANGE COLUMN GAME_TYPE GAME_TYPE VARCHAR(255) NULL DEFAULT '' , 
DROP PRIMARY KEY , 
ADD PRIMARY KEY (ACCOUNT_ID, AUDIT_DATE, PLATFORM)#

CREATE TABLE IF NOT EXISTS PLAYER_ACCOUNT_INFO (
  ACCOUNT_ID INT(11) NOT NULL ,
  REGISTRATION_PLATFORM VARCHAR(32) NULL ,
  REGISTRATION_GAME VARCHAR(64) NULL ,
  FIRST_PURCHASE DATETIME NULL ,
  PRIMARY KEY (ACCOUNT_ID) ,
  INDEX PLATFORM_KEY (REGISTRATION_PLATFORM ASC) )#

DROP TABLE IF EXISTS PLAYER_INFO#
DROP VIEW IF EXISTS PLAYER_INFO#

CREATE VIEW PLAYER_INFO AS
SELECT p.PLAYER_ID AS PLAYER_ID,pi.REGISTRATION_PLATFORM AS REGISTRATION_PLATFORM,
  pi.REGISTRATION_GAME AS REGISTRATION_GAME,pi.FIRST_PURCHASE AS FIRST_PURCHASE
FROM PLAYER_ACCOUNT_INFO pi LEFT JOIN strataprod.PLAYER p ON p.ACCOUNT_ID=pi.ACCOUNT_ID#

DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE account_inserts(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64))
BEGIN
  INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, REGISTRATION_PLATFORM)
      VALUES (account_id_val,platform_val)
      ON DUPLICATE KEY
      UPDATE REGISTRATION_PLATFORM = 
        IF(REGISTRATION_PLATFORM IS NULL,VALUES(REGISTRATION_PLATFORM),REGISTRATION_PLATFORM);
        
    INSERT IGNORE INTO rpt_activity_by_account_id(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,DATE(tsstarted_val),platform_val);
END
#

DROP TRIGGER IF EXISTS account_actions#
CREATE TRIGGER account_actions
AFTER INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  CALL account_inserts(NEW.ACCOUNT_ID,NEW.TSSTARTED,NEW.PLATFORM);
END
#

DROP PROCEDURE IF EXISTS extractAccountActivityTimed#
CREATE PROCEDURE extractAccountActivityTimed(IN runtime TIMESTAMP)
begin
	declare runtime timestamp;
	declare lastDistinctPlayersRuntime timestamp;
	declare lastAuditCommandAutoId int;
	declare maxAuditCommandAutoId int;
	declare lastDistinctUsersRuntime timestamp;
	declare startOfCurrentWeek date;
	declare startOfCurrentMonth date;
	declare lastCompleteWeekWeekEnding date default '2010-08-01';
	declare lastCompleteMonthEnding date default '2010-07-31';

    -- update player activity
    select val, action_ts into @lastAuditCommandAutoId, @lastDistinctPlayersRuntime from rpt_report_status where report_action = 'distinctPlayers';
    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;

	replace into rpt_account_activity (player_id, audit_date, game_type)
      select distinct account_id, date(audit_ts), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

	update rpt_report_status set action_ts = @runtime, val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';

    -- update users
    select action_ts into @lastDistinctUsersRuntime from rpt_report_status where report_action = 'distinctUsers';
	update rpt_report_status set action_ts = @runtime where report_action = 'distinctUsers';

    -- update daily distinct players by game_type
	replace into rpt_players_daily(reportDate, players, game_type)
      select audit_date, count(player_id), game_type
      from rpt_account_activity aa
      where aa.audit_date >= date(@lastDistinctPlayersRuntime)
        and aa.game_type != ''
      group by aa.audit_date, game_type;

    -- update daily distinct players
	replace into rpt_players_daily(reportDate, players, game_type)
      select audit_date, count(distinct player_id), ''
      from rpt_account_activity aa
      where aa.audit_date >= date(@lastDistinctPlayersRuntime)
        and aa.game_type != ''
      group by aa.audit_date;

    -- update daily distinct users (people who have played or logged on)
	replace into rpt_users_daily(reportDate, users)
      select audit_date, count(distinct account_id)
      from rpt_activity_by_account_id aa
      where aa.audit_date >= date(@lastDistinctUsersRuntime)
      group by aa.audit_date;

    -- update weekly distinct users
	set @startOfCurrentWeek = date(date_sub(@runtime, interval weekday(@runtime) day));
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_users_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_users_weekly(weekEnding, users)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct account_id)
          from rpt_activity_by_account_id aa
          where aa.audit_date > @lastCompleteWeekWeekEnding and aa.audit_date < @startOfCurrentWeek
          group by yearweek(aa.audit_date, 5);
	end if;

    -- update weekly distinct players
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_players_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_players_weekly(weekEnding, players, game_type)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct player_id), game_type
          from rpt_account_activity aa
          where aa.audit_date > @lastCompleteWeekWeekEnding
            and aa.audit_date < @startOfCurrentWeek
            and aa.game_type != ''
          group by yearweek(aa.audit_date, 5), game_type;

		replace into rpt_players_weekly(weekEnding, players, game_type)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct player_id), ''
          from rpt_account_activity aa
          where aa.audit_date > @lastCompleteWeekWeekEnding
            and aa.audit_date < @startOfCurrentWeek
            and aa.game_type != ''
          group by yearweek(aa.audit_date, 5);
	end if;


	set @endOfPreviousMonth = last_day(date_sub(@runtime, interval 1 month));

    -- update monthy distinct players
	select max(monthEnding) into @lastCompleteMonthEnding from rpt_players_monthly;
	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_players_monthly(monthEnding, players, game_type)
          select last_day(aa.audit_date), count(distinct player_id), game_type
          from rpt_account_activity aa
          where aa.audit_date > @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            and aa.game_type != ''
          group by DATE_FORMAT(aa.audit_date, '%m %Y'), game_type;

		replace into rpt_players_monthly(monthEnding, players, game_type)
          select last_day(aa.audit_date), count(distinct player_id), ''
          from rpt_account_activity aa
          where aa.audit_date > @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            and aa.game_type != ''
          group by DATE_FORMAT(aa.audit_date, '%m %Y');
	end if;

    -- update monthly distinct users
	select max(monthEnding) into @lastCompleteMonthEnding from rpt_users_monthly;
	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_users_monthly(monthEnding, users)
          select last_day(aa.audit_date), count(distinct account_id)
          from rpt_activity_by_account_id aa
          where aa.audit_date > @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
          group by DATE_FORMAT(aa.audit_date, '%m %Y');
	end if;
end;
#

DROP PROCEDURE IF EXISTS extractAccountActivity#
CREATE PROCEDURE extractAccountActivity()
begin
	CALL extractAccountActivityTimed(now());
end
#

-- Reapply also the external transactions-related procedures

DROP PROCEDURE IF EXISTS external_transactions_inserts#
CREATE PROCEDURE external_transactions_inserts(IN account_id_val INT(11), IN first_purchase_val DATETIME)
BEGIN
  INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, FIRST_PURCHASE)
    VALUES (account_id_val,first_purchase_val)
      ON DUPLICATE KEY
      UPDATE FIRST_PURCHASE = 
          IF(FIRST_PURCHASE IS NULL,VALUES(FIRST_PURCHASE),FIRST_PURCHASE);
END
#

DROP TRIGGER IF EXISTS external_transactions_actions#
CREATE TRIGGER external_transactions_actions
AFTER INSERT ON EXTERNAL_TRANSACTION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  CALL external_transactions_inserts(NEW.ACCOUNT_ID,NEW.MESSAGE_TIMESTAMP);
END#