DROP PROCEDURE rptExtractDistinctPlayers#
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
	join strataprod.TABLE_INFO ti on a.table_id = ti.table_id 
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

DROP PROCEDURE rptExtractExternalTransactions#
CREATE PROCEDURE `rptExtractExternalTransactions`()
BEGIN 
	DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL; 
	IF hour(now()) >= 2 THEN 
		SELECT MIN(reportDate) INTO @lastIncompleteDate 
		FROM rpt_external_transaction 
		WHERE complete = 0; 
		
		IF @lastIncompleteDate IS NOT NULL THEN 
			set @fromDate = @lastIncompleteDate; 
		ELSE 
			SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate 
			FROM rpt_external_transaction; 
		END IF; 
		
		DELETE FROM rpt_external_transaction where reportDate >= DATE(@fromDate); 
		
		INSERT rpt_external_transaction(reportDate, cashierName, results, amountUsd, amountEur, amountGbp, complete) 
		select p.message_timestamp, p.cashier_name, count(p.auto_id), sum((CURRENCY_CODE='USD')*p.AMOUNT), 
		sum((CURRENCY_CODE='EUR')*p.AMOUNT) , sum((CURRENCY_CODE='GBP')*p.AMOUNT), p.message_timestamp < date(now()) 
		from EXTERNAL_TRANSACTION p 
		where month(p.message_timestamp) <> 0 and external_transaction_status='SUCCESS' 
		and p.message_timestamp >= DATE(@fromDate) 
		group by p.cashier_name, date(p.message_timestamp) order by p.message_timestamp desc; 
	END IF; 
END;
#

drop procedure rptExtractDistinctUsersByGameSource#
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
                left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
              where s.tsstarted >= @fromDate
              group by s.ACCOUNT_ID, date(s.tsstarted)) t
        where t.insider=1
        group by date(t.lastDate)
        order by t.lastDate desc;
END;
#
drop procedure rptExtractDistinctUsersByGameSourceMonthly#
CREATE PROCEDURE `rptExtractDistinctUsersByGameSourceMonthly`()
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
                    left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where last_day(s.tsstarted) < last_day(curdate()) and s.tsstarted >= date_add(@fromDate, interval 1 day)
                  group by s.ACCOUNT_ID, DATE_FORMAT(s.tsstarted, '%m %Y')) t
            where t.insider=1
            group by DATE_FORMAT(lastDate, '%m %Y')
            order by t.lastDate desc;
    end if;
END;
#
drop procedure rptExtractDistinctUsersByGameSourceWeekly#
CREATE PROCEDURE `rptExtractDistinctUsersByGameSourceWeekly`()
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
                    left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where s.tsstarted >= date_add(@fromDate, interval 1 day) and s.tsstarted < @startOfCurrentWeek group by s.ACCOUNT_ID, yearweek(tsstarted, 5)) t
            where t.insider=1
            group by yearweek(lastdate, 5)
            order by t.lastDate desc;
    end if;
END;
#

