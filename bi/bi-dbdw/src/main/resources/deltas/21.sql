DROP TABLE IF EXISTS rpt_account_activity#

create table rpt_account_activity (
  PLAYER_ID bigint(20) not null,
  AUDIT_DATE date not null,
  GAME_TYPE varchar(255),
  primary key (PLAYER_ID, AUDIT_DATE, GAME_TYPE),
  key IDX_AUDIT_DATE (AUDIT_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#

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

	replace into rpt_account_activity (player_id, audit_date, game_type)
      select distinct account_id, date(audit_ts), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

	update rpt_report_status set action_ts = @runtime, val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';

    -- update users
    select val, action_ts into @lastAccountSessionSessionId, @lastDistinctUsersRuntime from rpt_report_status where report_action = 'distinctUsers';
	select max(session_id) into @maxAccountSessionSessionId from ACCOUNT_SESSION;

    insert ignore into rpt_account_activity (player_id, audit_date, game_type)
        select distinct player_id, date(tsstarted), ''
        from ACCOUNT_SESSION sess join strataprod.PLAYER p on sess.account_id = p.account_id
        where session_id > @lastAccountSessionSessionId and session_id <= @maxAccountSessionSessionId
          and not exists (select 1 from rpt_account_activity aa where aa.audit_date = date(tsstarted) and aa.player_id = p.player_id);

	update rpt_report_status set action_ts = @runtime, val = @maxAccountSessionSessionId where report_action = 'distinctUsers';

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
      select audit_date, count(distinct player_id)
      from rpt_account_activity aa
      where aa.audit_date >= date(@lastDistinctUsersRuntime)
      group by aa.audit_date;

    -- update weekly distinct users
	set @startOfCurrentWeek = date(date_sub(@runtime, interval weekday(@runtime) day));
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_users_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_users_weekly(weekEnding, users)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct player_id)
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteWeekWeekEnding and aa.audit_date < @startOfCurrentWeek
          group by yearweek(aa.audit_date, 5);
	end if;

    -- update weekly distinct players
	select max(weekEnding) into @lastCompleteWeekWeekEnding from rpt_players_weekly;
	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_players_weekly(weekEnding, players, game_type)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct player_id), game_type
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteWeekWeekEnding
            and aa.audit_date < @startOfCurrentWeek
            and aa.game_type != ''
          group by yearweek(aa.audit_date, 5), game_type;

		replace into rpt_players_weekly(weekEnding, players, game_type)
          select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct player_id), ''
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteWeekWeekEnding
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
          where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            and aa.game_type != ''
          group by DATE_FORMAT(aa.audit_date, '%m %Y'), game_type;

		replace into rpt_players_monthly(monthEnding, players, game_type)
          select last_day(aa.audit_date), count(distinct player_id), ''
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            and aa.game_type != ''
          group by DATE_FORMAT(aa.audit_date, '%m %Y');
	end if;

    -- update monthly distinct users
	select max(monthEnding) into @lastCompleteMonthEnding from rpt_users_monthly;
	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_users_monthly(monthEnding, users)
          select last_day(aa.audit_date), count(distinct player_id)
          from rpt_account_activity aa
          where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
          group by DATE_FORMAT(aa.audit_date, '%m %Y');
	end if;
end;
#
