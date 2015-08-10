drop procedure rptExtractDistinctPlayers#
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
    select action_ts into @lastRunTime from rpt_report_status where report_action = 'distinctPlayerExtractLastRun';
    set @fromDateTime = timestampadd(minute, -30, @lastRunTime);
    replace into rpt_tmp_table_activity (audit_date, account_id, table_id)
        select distinct date(audit_ts), account_id, table_id
        from strataprod.AUDIT_COMMAND
        where audit_ts > @fromDateTime and audit_ts <= @runtime and command_type not in ('Leave', 'GetStatus');
    update rpt_tmp_table_activity a join strataprod.TABLE_INFO ti on a.table_id = ti.table_id
        set a.slots=ti.game_type='SLOTS', a.roulette=ti.game_type='ROULETTE', a.blackjack=ti.game_type='BLACKJACK',
            a.texas_holdem=ti.game_type='TEXAS_HOLDEM',
            a.other=(ti.GAME_TYPE<>'BLACKJACK' and ti.GAME_TYPE<>'TEXAS_HOLDEM' and ti.GAME_TYPE<>'ROULETTE' and ti.GAME_TYPE<>'SLOTS');
    replace into rpt_tmp_account_activity (account_id, audit_date, slots, texas_holdem, blackjack, roulette, other)
        select account_id, audit_date, if(sum(slots)>0, account_id, null) account_id, if(sum(texas_holdem)>0,account_id,null),
            if(sum(blackjack)>0,account_id,null), if(sum(roulette)>0,account_id,null), if(sum(other)>0, account_id, null)
        from rpt_tmp_table_activity
        group by account_id, audit_date;
    replace into rpt_distinct_players(reportDate, players, slots, texas, roulette, blackjack, other)
        select audit_date, count(account_id), count(distinct slots), count(distinct texas_holdem), count(distinct roulette),
            count(distinct blackjack), count(distinct other)
        from rpt_tmp_account_activity aa
        where aa.audit_date >= date(@fromDateTime)
        group by aa.audit_date;
    if date(@runtime) > date(@fromDateTime) then
        delete from rpt_tmp_table_activity where audit_date <= date(date_sub(@runtime, interval 1 day));
    end if;
    update rpt_report_status set action_ts = @runtime
        where report_action = 'distinctPlayerExtractLastRun';
    set @fromDateTime = timestampadd(minute, -30, @runtime);
    set @startOfCurrentWeek = date(date_sub(@fromDateTime, interval weekday(@fromDateTime) day));
    select action_ts into @lastCompleteWeekWeekEnding
    from rpt_report_status
    where report_action = 'distinctPlayerExtractLastWeekEnding';
    if date(date_add(@lastCompleteWeekWeekEnding, interval 1 week)) < @startOfCurrentWeek then
        replace into rpt_distinct_players_weekly(weekEnding, players, slots, texas, roulette, blackjack, other)
            select date(date_add(aa.audit_date, interval (6 - weekday(aa.audit_date)) day)), count(distinct account_id),
                count(distinct slots), count(distinct texas_holdem), count(distinct roulette), count(distinct blackjack),
                count(distinct other)
            from rpt_tmp_account_activity aa
            where aa.audit_date >= @lastCompleteWeekWeekEnding and aa.audit_date < @startOfCurrentWeek group by yearweek(aa.audit_date, 5);
        update rpt_report_status set action_ts = date(date_add(@startOfCurrentWeek, interval -1 day))
            where report_action = 'distinctPlayerExtractLastWeekEnding';
    end if;
    select date(action_ts) into @lastCompleteMonthEnding from rpt_report_status where report_action = 'distinctPlayerExtractLastMonthEnding';
    set @endOfPreviousMonth = last_day(date_sub(@fromDateTime, interval 1 month));
    if (@lastCompleteMonthEnding < @endOfPreviousMonth) then
        replace into rpt_distinct_players_monthly(monthEnding, players, slots, texas, roulette, blackjack, other)
            select last_day(aa.audit_date), count(distinct account_id), count(distinct slots), count(distinct texas_holdem),
                count(distinct roulette), count(distinct blackjack), count(distinct other)
            from rpt_tmp_account_activity aa
            where aa.audit_date >= @lastCompleteMonthEnding and aa.audit_date <= @endOfPreviousMonth
            group by DATE_FORMAT(aa.audit_date, '%m %Y');
        update rpt_report_status set action_ts = @endOfPreviousMonth where report_action = 'distinctPlayerExtractLastMonthEnding';
    end if;
end;
#
