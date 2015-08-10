
/********************************************************************************
 **      PROC: fillDailyMailStats                                              **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS fillDailyMailStats
#

CREATE PROCEDURE fillDailyMailStats()
BEGIN
	declare runtime timestamp;

	if get_lock('strataproddw.fill_daily_mail_2', 0) = 1 then
		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Users' AS ACTIVITY,0 AS ORD,count(DISTINCT ACCOUNT_ID) as Number,
				CAST(sum(case PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS
				from rpt_activity_by_account_id where AUDIT_DATE = (curdate() - interval 1 day);

		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Players' AS ACTIVITY,1 AS ORD,count(DISTINCT PLAYER_ID) as Number,
				CAST(sum(case PARTNER_ID when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PARTNER_ID when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PARTNER_ID when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS
				from UNIQUE_PLAYERS_YESTERDAY;

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			SELECT Date(now()) AS AUDIT_DATE,'New accounts' AS ACQUISITION,0 AS ORD,count(p.player_id) as Number,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS
				FROM strataproddw.PLAYER p
				LEFT JOIN strataproddw.PLAYER_ACCOUNT_INFO pi ON p.ACCOUNT_ID=pi.ACCOUNT_ID
				WHERE p.tscreated > (cast(now() as date) - interval 1 day) and p.tscreated < cast(now() as date);

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Invitations sent' AS ACQUISITION,1 AS ORD,count(*) as Number,
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS from strataproddw.INVITATIONS
				where CREATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Invitations accepted' AS ACQUISITION,2 AS ORD,count(*) as Number,
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS
				from strataproddw.INVITATIONS
				where (STATUS = 'ACCEPTED' or STATUS = 'ACCEPTED_OTHER')
				AND UPDATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);

		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Purchases' AS PURCHASES,0 AS ORD,count(*) AS Number,NULL AS Facebook,NULL AS Yazino,NULL AS iOS
			from strataproddw.EXTERNAL_TRANSACTION
			WHERE MESSAGE_TIMESTAMP BETWEEN (cast(now() as date) - interval 1 day)
			AND cast(now() as date) AND EXTERNAL_TRANSACTION_STATUS='SUCCESS';

		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Buyers' AS PURCHASES,1 AS ORD,count(DISTINCT account_id) as Number,
				CAST(sum(case PARTNER_ID when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PARTNER_ID when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PARTNER_ID when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS
				from UNIQUE_BUYERS_YESTERDAY;

		REPLACE INTO rpt_mv_stats_games(AUDIT_DATE,GAME,Players,Games)
			select Date(now()) AS AUDIT_DATE,game_type AS GAME,players AS Players,games AS Games from GAME_STATS_YESTERDAY;

		REPLACE INTO rpt_mv_stats_tournaments(AUDIT_DATE,TOURNAMENT,Players,Tournaments)
			select Date(now()) AS AUDIT_DATE,GAME_TYPE AS TOURNAMENT,UNIQUE_PLAYERS AS Players,TOURNAMENTS AS Tournaments
				from UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY;

		do release_lock('strataproddw.fill_daily_mail_2');
	end if;
END
#




/********************************************************************************
 **      PROC: account_inserts                                                 **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS account_inserts
#

CREATE PROCEDURE account_inserts(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64), IN start_page_val VARCHAR(2048))
BEGIN
	DECLARE user_id_val BIGINT(20) DEFAULT 0;
	declare maxUserId BIGINT(20) DEFAULT 0;

	SELECT MAX(USER_ID) INTO @maxUserId FROM rpt_recent_registrations;

	IF NOT @maxUserId IS NULL THEN
		INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
    	SELECT USER_ID,TSREG,PROVIDER_NAME,IF(PROVIDER_NAME='YAZINO',USER_ID,EXTERNAL_ID),account_id_val,FIRST_NAME
    		FROM strataproddw.LOBBY_USER
    		WHERE USER_ID > @maxUserId;
    ELSE
    	INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
    	SELECT USER_ID,TSREG,PROVIDER_NAME,EXTERNAL_ID,account_id_val,FIRST_NAME
    		FROM strataproddw.LOBBY_USER
    		WHERE TSREG > now() - INTERVAL 90 MINUTE;
    END IF;

	SELECT `lu`.`USER_ID`
	INTO @user_id_val
	FROM
	  strataproddw.PLAYER `p`
	  join strataproddw.LOBBY_USER `lu`
	WHERE
	  ACCOUNT_ID = account_id_val ORDER BY `lu`.`USER_ID` DESC LIMIT 1;

	UPDATE rpt_recent_registrations SET PLATFORM = platform_val, START_PAGE = start_page_val WHERE USER_ID = @user_id_val AND PLATFORM = '';

  	INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, REGISTRATION_PLATFORM)
      VALUES (account_id_val,platform_val)
      ON DUPLICATE KEY
      UPDATE REGISTRATION_PLATFORM =
        IF(REGISTRATION_PLATFORM IS NULL,VALUES(REGISTRATION_PLATFORM),REGISTRATION_PLATFORM);

    INSERT IGNORE INTO rpt_activity_by_account_id(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,DATE(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,DATE(tsstarted_val),'');

    INSERT IGNORE INTO rpt_activity_by_account_id_weekly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day_of_week(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day_of_week(tsstarted_val),'');

    INSERT IGNORE INTO rpt_activity_by_account_id_monthly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day(tsstarted_val),'');
END;
#




/********************************************************************************
 **      PROC: calc_agg_transaction_log_by_date_gvt_acc                        **
 ********************************************************************************/

drop procedure if exists `strataproddw`.`calc_agg_transaction_log_by_date_gvt_acc`
#

create procedure `strataproddw`.`calc_agg_transaction_log_by_date_gvt_acc`(in transaction_id_from bigint, in transaction_id_to bigint)
begin
    insert into AGG_TRANSACTION_LOG_BY_DATE_GVT_ACC
        select
            date(transaction_ts) date,
            game_variation_template_id,
            account_id,
            sum(if(amount < 0, -amount, 0))  total_stake,
            sum(if(amount < 0, 1, 0)) total_num_stakes,
            sum(if(amount > 0, amount, 0)) total_return,
            sum(if(amount > 0, 1, 0)) total_num_returns,
            count(transaction_log_id) transactions
        from TRANSACTION_LOG tl, strataproddw.TABLE_INFO ti
        where substring_index(reference, '|', 1) = table_id
        and transaction_log_id > transaction_id_from
        and transaction_log_id <= transaction_id_to
        and transaction_type in ('Stake', 'Return')
        and amount <> 0
        group by date(transaction_ts), game_variation_template_id, account_id
    on duplicate key update
        total_stake = total_stake + values(total_stake),
        total_num_stakes = total_num_stakes + values(total_num_stakes),
        total_return = total_return + values(total_return),
        total_num_returns = total_num_returns + values(total_num_returns),
        total_transactions = total_transactions + values(total_transactions);
end
#




/********************************************************************************
 **      PROC: extractLastPlayed                                               **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS `strataproddw`.`extractLastPlayed`
#

CREATE PROCEDURE extractLastPlayed()
begin
	declare runtime timestamp;
	declare lastAutoId int;
	declare maxAutoId int;

	set @runtime = now();

	select action_ts, val into @lastRunTime, @lastAutoId from rpt_report_status where report_action = 'extractLastPlayed';
    select max(auto_id) into @maxAutoId from AUDIT_COMMAND;

	replace into LAST_PLAYED (player_id, game_type, last_played)
	  select account_id, game_type, max(audit_ts)
      from AUDIT_COMMAND ac join strataproddw.TABLE_INFO ti on ac.table_id = ti.table_id
      where ac.auto_id > @lastAutoId and ac.auto_id <= @maxAutoId
        and command_type not in ('Leave', 'GetStatus')
      group by account_id, game_type;

	update rpt_report_status set action_ts = @runtime, val = @maxAutoId where report_action = 'extractLastPlayed';
end;
#




/********************************************************************************
 **      PROC: rptExtractDistinctPlayers                                       **
 ********************************************************************************/

DROP PROCEDURE rptExtractDistinctPlayers
#

CREATE PROCEDURE `rptExtractDistinctPlayers`()
begin
	declare lastRuntime timestamp default '2010-08-23';
	declare fromDateTime timestamp;
	declare runtime timestamp;
	declare startOfCurrentWeek date;
	declare startOfCurrentMonth date;
	declare lastCompleteWeekWeekEnding date default '2010-08-01';
	declare lastCompleteMonthEnding date default '2010-07-31';

	set @runtime = now();
	select action_ts into @lastRunTime
	from rpt_report_status where report_action = 'distinctPlayerExtractLastRun';

	set @fromDateTime = timestampadd(minute, -30, @lastRunTime);

	replace into rpt_tmp_table_activity (audit_date, account_id, table_id)
	select distinct date(audit_ts), account_id, table_id
	from AUDIT_COMMAND
	where audit_ts > @fromDateTime and audit_ts <= @runtime
	and command_type not in ('Leave', 'GetStatus');

	update rpt_tmp_table_activity a
	join strataproddw.TABLE_INFO ti on a.table_id = ti.table_id
	set a.slots=ti.game_type='SLOTS', a.roulette=ti.game_type='ROULETTE',
	a.blackjack=ti.game_type='BLACKJACK', a.texas_holdem=ti.game_type='TEXAS_HOLDEM',
	a.other=(ti.GAME_TYPE<>'BLACKJACK' and ti.GAME_TYPE<>'TEXAS_HOLDEM' and ti.GAME_TYPE<>'ROULETTE' and ti.GAME_TYPE<>'SLOTS');

	replace into rpt_tmp_account_activity (account_id, audit_date, slots, texas_holdem, blackjack, roulette, other)
	select account_id, audit_date, if(sum(slots)>0, account_id, null) account_id, if(sum(texas_holdem)>0,account_id,null),
	if(sum(blackjack)>0,account_id,null), if(sum(roulette)>0,account_id,null), if(sum(other)>0, account_id, null)
	from rpt_tmp_table_activity
	group by account_id, audit_date;

	replace into rpt_distinct_players(reportDate, players, slots, texas, roulette, blackjack, other)
	select audit_date, count(account_id), count(distinct slots), count(distinct texas_holdem),
	count(distinct roulette), count(distinct blackjack), count(distinct other)
	from rpt_tmp_account_activity aa where aa.audit_date >= date(@fromDateTime)
	group by aa.audit_date;

	if date(@runtime) > date(@fromDateTime) then
		delete from rpt_tmp_table_activity where audit_date <= date(date_sub(@runtime, interval 1 day));
	end if;

	update rpt_report_status set action_ts = @runtime
	where report_action = 'distinctPlayerExtractLastRun';

	set @fromDateTime = timestampadd(minute, -30, @runtime);
	set @startOfCurrentWeek = date(date_sub(@fromDateTime, interval weekday(@fromDateTime) day));

	select action_ts into @lastCompleteWeekWeekEnding
	from rpt_report_status where report_action = 'distinctPlayerExtractLastWeekEnding';

	if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
		replace into rpt_distinct_players_weekly(weekEnding, players, slots, texas, roulette, blackjack, other)
		select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct account_id),
		count(distinct slots), count(distinct texas_holdem), count(distinct roulette), count(distinct blackjack),
		count(distinct other)
		from rpt_tmp_account_activity aa
		where aa.audit_date >= @lastCompleteWeekWeekEnding and aa.audit_date < @startOfCurrentWeek
		group by yearweek(aa.audit_date, 5);

		update rpt_report_status set action_ts = date(date_add(@startOfCurrentWeek, interval -1 day))
		where report_action = 'distinctPlayerExtractLastWeekEnding';
	end if;

	select date(action_ts) into @lastCompleteMonthEnding from rpt_report_status
	where report_action = 'distinctPlayerExtractLastMonthEnding';

	set @endOfPreviousMonth = last_day(date_sub(@fromDateTime, interval 1 month));

	if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
		replace into rpt_distinct_players_monthly(monthEnding, players, slots, texas, roulette, blackjack, other)
		select last_day(aa.audit_date), count(distinct account_id), count(distinct slots), count(distinct texas_holdem),
		count(distinct roulette), count(distinct blackjack), count(distinct other)
		from rpt_tmp_account_activity aa
		where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
		group by DATE_FORMAT(aa.audit_date, '%m %Y');

		update rpt_report_status set action_ts = @endOfPreviousMonth
		where report_action = 'distinctPlayerExtractLastMonthEnding';
	end if;
end;
#




/********************************************************************************
 **      PROC: rptExtractDistinctUsersByGameSource                             **
 ********************************************************************************/

drop procedure rptExtractDistinctUsersByGameSource
#

CREATE PROCEDURE `rptExtractDistinctUsersByGameSource`()
begin
    declare lastIncompleteDate, fromDate date default null;
    select MIN(reportDate) into @lastIncompleteDate from rpt_distinct_users_by_game_source where complete = false;
    if @lastIncompleteDate is not NULL then
        set @fromDate = @lastIncompleteDate;
    else
        select date(ifnull(max(reportDate), '2000-01-01'))  into @fromDate from rpt_distinct_users_by_game_source;
    end if;
    delete from rpt_distinct_users_by_game_source where reportDate >= @fromDate;
    insert into rpt_distinct_users_by_game_source(reportDate, users, slotsUsers, texasUsers, rouletteUsers, blackjackUsers, otherUsers, complete)
        select date(t.lastDate) reportDate, count(distinct t.account_id) users, sum(slots>0) slotsUsers, sum(texasholdem>0) texasUsers,
            sum(roulette>0) rouletteUsers, sum(blackjack>0) blackjackUsers, sum(others>0) otherUsers, t.lastDate < date(now()) complete
        from (select max(s.tsstarted) lastDate, s.account_id,
                case when p.is_insider is null then 1 else 0 end insider,
                ifnull(sum(instr(s.referer, 'slots') > 0), 0) slots,
                ifnull(sum(instr(s.referer, 'holdem') > 0),0) texasholdem,
                ifnull(sum(instr(s.referer, 'roulette') > 0),0) roulette,
                ifnull(sum(instr(s.referer, 'blackjack') > 0),0) blackjack,
                sum((s.referer is null) or (instr(s.referer, 'slots') = 0 and instr(s.referer, 'holdem') = 0 and instr(s.referer, 'roulette') = 0 and instr(s.referer, 'blackjack') = 0)) others
              from ACCOUNT_SESSION s
                left join strataproddw.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
              where s.tsstarted >= @fromDate
              group by s.ACCOUNT_ID, date(s.tsstarted)) t
        where t.insider=1
        group by date(t.lastDate)
        order by t.lastDate desc;
END;
#




/********************************************************************************
 **      PROC: rptExtractDistinctUsersByGameSourceMonthly                      **
 ********************************************************************************/

drop procedure if exists `strataproddw`.`rptExtractDistinctUsersByGameSourceMonthly`
#

CREATE PROCEDURE `strataproddw`.`rptExtractDistinctUsersByGameSourceMonthly`()
begin
    declare fromDate date default null;
    select date(ifnull(max(monthEnding), '2000-01-01')) into @fromDate
    from rpt_distinct_users_by_game_source_monthly;
    if (@fromDate < date_add(curdate(), interval -1 month)) then
        insert into rpt_distinct_users_by_game_source_monthly(monthEnding, users, slotsUsers, texasUsers, rouletteUsers, blackjackUsers, otherUsers)
            select last_day(t.lastDate) monthEnding, count(distinct t.account_id) users, sum(slots>0) slotsUsers,
                sum(texasholdem>0) texasUsers, sum(roulette>0) rouletteUsers, sum(blackjack>0) blackjackUsers, sum(others>0) otherUsers
            from (select max(s.tsstarted) lastDate, s.account_id, case when p.is_insider is null then 1 else 0 end insider,
                    ifnull(sum(instr(s.referer, 'slots') > 0), 0) slots, ifnull(sum(instr(s.referer, 'holdem') > 0),0) texasholdem,
                    ifnull(sum(instr(s.referer, 'roulette') > 0),0) roulette, ifnull(sum(instr(s.referer, 'blackjack') > 0),0) blackjack,
                    sum((s.referer is null) or (instr(s.referer, 'slots') = 0 and instr(s.referer, 'holdem') = 0
                        and instr(s.referer, 'roulette') = 0 and instr(s.referer, 'blackjack') = 0)) others
                  from ACCOUNT_SESSION s
                    left join strataproddw.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where last_day(s.tsstarted) < last_day(curdate()) and s.tsstarted >= date_add(@fromDate, interval 1 day)
                  group by s.ACCOUNT_ID, DATE_FORMAT(s.tsstarted, '%m %Y')) t
            where t.insider=1
            group by DATE_FORMAT(lastDate, '%m %Y')
            order by t.lastDate desc;
    end if;
END
#




/********************************************************************************
 **      PROC: rptExtractDistinctUsersByGameSourceWeekly                       **
 ********************************************************************************/

drop procedure if exists `strataproddw`.`rptExtractDistinctUsersByGameSourceWeekly`
#

CREATE PROCEDURE `strataproddw`.`rptExtractDistinctUsersByGameSourceWeekly`()
begin
    declare fromDate, startOfCurrentWeek date default null;
    select date(ifnull(max(weekEnding), '2000-01-01')) into @fromDate from rpt_distinct_users_by_game_source_weekly;
    if (@fromDate < date_sub(curdate(), interval weekday(curdate()) day)) then
        set @startOfCurrentWeek = date_sub(curdate(), interval weekday(curdate()) day);
        insert into rpt_distinct_users_by_game_source_weekly(weekEnding, users, slotsUsers, texasUsers, rouletteUsers, blackjackUsers, otherUsers)
            select date(date_add(t.lastDate, interval (6 - weekday(t.lastDate)) day)) weekEnding,
                count(distinct t.account_id) users, sum(slots>0) slotsUsers, sum(texasholdem>0) texasUsers,
                sum(roulette>0) rouletteUsers, sum(blackjack>0) blackjackUsers, sum(others>0) otherUsers
            from (select s.tsstarted lastDate, s.account_id, case when p.is_insider is null then 1 else 0 end insider,
                    ifnull(sum(instr(s.referer, 'slots') > 0), 0) slots, ifnull(sum(instr(s.referer, 'holdem') > 0),0) texasholdem,
                    ifnull(sum(instr(s.referer, 'roulette') > 0),0) roulette, ifnull(sum(instr(s.referer, 'blackjack') > 0),0) blackjack,
                    sum((s.referer is null) or (instr(s.referer, 'slots') = 0 and instr(s.referer, 'holdem') = 0 and
                        instr(s.referer, 'roulette') = 0 and instr(s.referer, 'blackjack') = 0)) others
                  from ACCOUNT_SESSION s
                    left join strataproddw.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where s.tsstarted >= date_add(@fromDate, interval 1 day) and s.tsstarted < @startOfCurrentWeek group by s.ACCOUNT_ID, yearweek(tsstarted, 5)) t
            where t.insider=1
            group by yearweek(lastdate, 5)
            order by t.lastDate desc;
    end if;
END;
#




/********************************************************************************
 **      PROC: rptExtractFacebookNewRegistrations                              **
 ********************************************************************************/

drop procedure if exists `strataproddw`.`rptExtractFacebookNewRegistrations`
#

CREATE PROCEDURE `strataproddw`.`rptExtractFacebookNewRegistrations`()
BEGIN
    DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL;
    IF hour(now()) >= 2 THEN
        SELECT MIN(reportDate) INTO @lastIncompleteDate FROM rpt_facebook_registration WHERE complete = 0;
        IF @lastIncompleteDate IS NOT NULL THEN
            set @fromDate = @lastIncompleteDate;
        ELSE
            SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate
            FROM rpt_facebook_registration;
        END IF;
        DELETE FROM rpt_facebook_registration where reportDate >= DATE(@fromDate);
        INSERT rpt_facebook_registration(reportDate, registrations, complete)
            SELECT p.tscreated, count(p.player_id), p.tscreated < date(now())
            FROM strataproddw.PLAYER p
                left join strataproddw.LOBBY_USER u on p.player_id = u.player_id
            WHERE month(p.tscreated) <> 0 and u.provider_name = 'FACEBOOK' AND p.tscreated >= DATE(@fromDate)
            GROUP BY date(p.tscreated)
            ORDER BY p.tscreated desc;
    END IF;
END;
#




/********************************************************************************
 **      PROC: rptFillSourcesTable                                             **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS `strataproddw`.`rptFillSourcesTable`
#

CREATE PROCEDURE `strataproddw`.`rptFillSourcesTable`()
BEGIN
	DECLARE lastSourcesId BIGINT(20) DEFAULT NULL;

	SELECT MAX(USER_ID) FROM rpt_player_sources_mv INTO	@lastSourcesId;

	if get_lock('strataproddw.fill_sources_tables_lock', 0) = 1 then
	  INSERT IGNORE INTO rpt_player_sources_mv(USER_ID,SOURCE,TSCREATED)
		SELECT l.USER_ID,
		CASE WHEN a.AD_CODE IS NULL THEN (CASE WHEN l.REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE a.AD_CODE END AS SOURCE,
		CASE WHEN a.REGISTRATION_TS IS NULL THEN p.tscreated ELSE a.REGISTRATION_TS END AS TSCREATED
		FROM strataproddw.PLAYER p
		JOIN strataproddw.LOBBY_USER l ON l.PLAYER_ID=p.PLAYER_ID
		LEFT JOIN AD_TRACKING a ON a.USER_ID=l.USER_ID
		WHERE l.USER_ID >= @lastSourcesId - 25;

		do release_lock('strataproddw.fill_sources_tables_lock');
	end if;
END
#




/********************************************************************************
 **      PROC: popLastPlayed                                                   **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS popLastPlayed#
CREATE PROCEDURE popLastPlayed()
begin
	declare runtime timestamp;
	declare maxId, currentId, batchSize, lastId int;

	set @batchSize = 1000;
	set @currentId = 0;
	select max(auto_id) into @maxId from AUDIT_COMMAND;

	while @currentId < @maxId do
		start transaction;
	    set @runtime = now();
	  	set @lastId = if(@currentId + @batchSize < @maxId, @currentId + @batchSize, @maxId);
		replace into LAST_PLAYED (account_id, game_type, last_played)
	  		select account_id, game_type, max(audit_ts)
      		from AUDIT_COMMAND ac join strataproddw.TABLE_INFO ti on ac.table_id = ti.table_id
      		where ac.auto_id > @currentId and ac.auto_id <= @lastId
              and command_type not in ('Leave', 'GetStatus')
      		group by account_id, game_type;
	  	set @currentId = @lastId;
	    update rpt_report_status set action_ts = @runtime, val = @lastId where report_action = 'extractLastPlayed';
      	commit;
	end while;
end;
#




/********************************************************************************
 **      PROC: popAccountActivity                                              **
 ********************************************************************************/

DROP PROCEDURE IF EXISTS popAccountActivity#
CREATE PROCEDURE popAccountActivity()
begin
	declare runtime timestamp;
	declare maxId, currentId, batchSize, lastId int;
	declare currentDate date;

	declare lastDistinctPlayersRuntime timestamp;
	declare lastAuditCommandAutoId int;

	select min(audit_date) into @currentDate from rpt_tmp_account_activity where audit_date <> '0000-00-00';

	while @currentDate < current_date do
		start transaction;
		replace into rpt_account_activity (player_id, audit_date, game_type)
			select account_id, audit_date, 'SLOTS'
			from rpt_tmp_account_activity
			where audit_date = @currentDate
			  and SLOTS is not null;

		replace into rpt_account_activity (player_id, audit_date, game_type)
			select account_id, audit_date, 'ROULETTE'
			from rpt_tmp_account_activity
			where audit_date = @currentDate
			  and ROULETTE is not null;

		replace into rpt_account_activity (player_id, audit_date, game_type)
			select account_id, audit_date, 'BLACKJACK'
			from rpt_tmp_account_activity
			where audit_date = @currentDate
			  and BLACKJACK is not null;


		replace into rpt_account_activity (player_id, audit_date, game_type)
			select account_id, audit_date, 'TEXAS_HOLDEM'
			from rpt_tmp_account_activity
			where audit_date = @currentDate
			  and TEXAS_HOLDEM is not null;

		replace into rpt_account_activity (player_id, audit_date, game_type)
			select account_id, audit_date, 'HISSTERIA'
			from rpt_tmp_account_activity
			where audit_date = @currentDate
			  and OTHER is not null;
		commit;
		set @currentDate = adddate(@currentDate, 1);
	end while;

	select max(auto_id), max(audit_ts) into @lastAuditCommandAutoId, @lastDistinctPlayersRuntime
	from AUDIT_COMMAND ac
	where ac.audit_ts = (
	  select max(audit_ts) from AUDIT_COMMAND
	  where audit_ts < current_date
	);

	update rpt_report_status set val = @lastAuditCommandAutoId, action_ts = @lastDistinctPlayersRuntime where report_action = 'distinctPlayers';

	set @batchSize = 1000;
	set @runtime = now();
    set @currentId = -1;
	select max(session_id) into @maxId from ACCOUNT_SESSION where tsstarted < current_date;

	while @currentId < @maxId do
		start transaction;
	    set @runtime = now();
	  	set @lastId = if(@currentId + @batchSize < @maxId, @currentId + @batchSize, @maxId);
		insert ignore into rpt_account_activity (player_id, audit_date, game_type)
			select distinct player_id, date(tsstarted), ''
			from ACCOUNT_SESSION sess join strataproddw.PLAYER p on sess.account_id = p.account_id
			where session_id > @currentId and session_id <= @lastId
			  and not exists (select 1 from rpt_account_activity ac where ac.audit_date = date(tsstarted) and ac.player_id = p.player_id);

	  	set @currentId = @lastId;
		update rpt_report_status set action_ts = @runtime, val = @lastId where report_action = 'distinctUsers';
      	commit;
	end while;
	update rpt_report_status set action_ts = @runtime, val = @maxId where report_action = 'distinctUsers';
end;
#
