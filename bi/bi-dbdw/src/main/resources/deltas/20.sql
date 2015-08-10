drop table if exists rpt_players_daily#
CREATE TABLE `rpt_players_daily` (
  `reportDate` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `game_type` varchar(255) not null,
  PRIMARY KEY (`reportDate`, game_type)
) ENGINE=InnoDB#

drop table if exists rpt_players_monthly #
CREATE TABLE `rpt_players_monthly` (
  `monthEnding` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `game_type` varchar(255) not null,
  PRIMARY KEY (`monthEnding`, game_type)
) ENGINE=InnoDB#

drop table if exists rpt_players_weekly#
CREATE TABLE `rpt_players_weekly` (
  `weekEnding` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `game_type` varchar(255) not null,
  PRIMARY KEY (`weekEnding`, game_type)
) ENGINE=InnoDB#

drop table if exists rpt_users_daily#
CREATE TABLE `rpt_users_daily` (
  `reportDate` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`reportDate`)
) ENGINE=InnoDB#

drop table if exists rpt_users_weekly#
CREATE TABLE `rpt_users_weekly` (
  `weekEnding` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`weekEnding`)
) ENGINE=InnoDB#

drop table if exists rpt_users_monthly#
CREATE TABLE `rpt_users_monthly` (
  `monthEnding` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`monthEnding`)
) ENGINE=InnoDB#


replace into rpt_report_status values ('extractLastPlayed', '2011-03-01 00:00:00', -1, 'last AUDIT_COMMAND processed (auto_id)')#
replace into rpt_report_status values ('distinctUsers', '2011-03-01 00:00:00', -1, 'last ACCOUNT_SESSION.session_id processed')#
replace into rpt_report_status values ('distinctPlayers', '2011-03-01 00:00:00', -1, 'last AUDIT_COMMAND.auto_id processed')#

--
-- Modify last played to work off auto_id (sequence number) rather than date
--
DROP PROCEDURE IF EXISTS extractLastPlayed#
CREATE PROCEDURE extractLastPlayed()
begin
	declare runtime timestamp;
	declare lastAutoId int;
	declare maxAutoId int;

	set @runtime = now();

	select action_ts, val into @lastRunTime, @lastAutoId from rpt_report_status where report_action = 'extractLastPlayed';
    select max(auto_id) into @maxAutoId from AUDIT_COMMAND;

	replace into LAST_PLAYED (account_id, game_type, last_played)
	  select account_id, game_type, max(audit_ts)
      from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
      where ac.auto_id > @lastAutoId and ac.auto_id <= @maxAutoId
        and command_type not in ('Leave', 'GetStatus')
      group by account_id, game_type;

	update rpt_report_status set action_ts = @runtime, val = @maxAutoId where report_action = 'extractLastPlayed';
end;
#


drop table if exists rpt_daily_table_activity#
--  --
-- Extracts distinct players per game and distinct users
--
DROP PROCEDURE IF EXISTS extractAccountActivity#
CREATE PROCEDURE extractAccountActivity()
begin
	declare runtime timestamp;
	declare lastDistinctPlayersRuntime timestamp;
	declare lastAuditCommandAutoId int;
	declare maxAuditCommandAutoId int;
	declare lastAccountSessionSessionId int;
	declare maxAccountSessionSessionId int;
	declare lastDistinctUsersRuntime timestamp;
	declare startOfCurrentWeek date;
	declare startOfCurrentMonth date;
	declare lastCompleteWeekWeekEnding date default '2010-08-01';
	declare lastCompleteMonthEnding date default '2010-07-31';

	set @runtime = now();

    -- update player activity
    select val, action_ts into @lastAuditCommandAutoId, @lastDistinctPlayersRuntime from rpt_report_status where report_action = 'distinctPlayers';
    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;

	replace into rpt_account_activity (account_id, audit_date, game_type)
      select account_id, date(audit_date), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and autoId <= maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

	update rpt_report_status set action_ts = @runtime, val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';

    -- update users
    select val action_ts into @lastAccountSessionSessionId, @lastDistinctUsersRuntime from rpt_report_status where report_action = 'distinctUsers';
	select max(session_id) into @maxAccountSessionSessionId from ACCOUNT_SESSION;

    insert ignore into rpt_account_activity (account_id, audit_date, game_type)
        select distinct account_id, date(tsstarted), ''
        from ACCOUNT_SESSION sess
        where session_id > @lastAccountSessionSessionId and session_id <= @maxAccountSessionSessionId
          and not exists (select 1 from rpt_account_activity ac where ac.audit_date = date(tsstarted) and ac.account_id = sess.account_id);

	update rpt_report_status set action_ts = @runtime, val = @maxAccountSessionSessionId where report_action = 'distinctUsers';

    -- update daily distinct players
	replace into rpt_players_daily(reportDate, players, game_type)
      select audit_date, count(account_id), game_type
      from rpt_account_activity aa
      where aa.audit_date >= date(@lastDistinctPlayersRuntime)
        and aa.game_type != ''
      group by aa.audit_date, game_type;

    -- update daily distinct users (people who have played or logged on)
	replace into rpt_users_daily(reportDate, users)
      select audit_date, count(distinct account_id)
      from rpt_account_activity aa
      where aa.audit_date >= date(@lastDistinctUsersRuntime)
      group by aa.audit_date;

    -- update weekly distinct users
	set @startOfCurrentWeek = date(date_sub(@runtime, interval weekday(@runtime) day));
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_users_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_users_weekly(weekEnding, users)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct account_id)
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteWeekWeekEnding and aa.audit_date < @startOfCurrentWeek
          group by yearweek(aa.audit_date, 5);
	end if;

    -- update weekly distinct players
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_players_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_players_weekly(weekEnding, players, game_type)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct account_id)
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteWeekWeekEnding
            and aa.audit_date < @startOfCurrentWeek
            and aa.game_type != ''
          group by yearweek(aa.audit_date, 5), game_type;
	end if;


	set @endOfPreviousMonth = last_day(date_sub(@runtime, interval 1 month));

    -- update monthy distinct players
	select max(monthEnding) into @lastCompleteMonthEnding from rpt_players_monthly;
	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_players_monthly(monthEnding, players, game_type)
          select last_day(aa.audit_date), count(distinct account_id), game_type
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            and aa.game_type != ''
          group by DATE_FORMAT(aa.audit_date, '%m %Y'), game_type;
	end if;

    -- update monthly distinct users
	select max(monthEnding) into @lastCompleteMonthEnding from rpt_users_monthly;
	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_users_monthly(monthEnding, users)
          select last_day(aa.audit_date), count(distinct account_id)
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
          group by DATE_FORMAT(aa.audit_date, '%m %Y');
	end if;
end;
#
