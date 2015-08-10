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
              from strataprod.ACCOUNT_SESSION s
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
                  from strataprod.ACCOUNT_SESSION s
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
                  from strataprod.ACCOUNT_SESSION s
                    left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where s.tsstarted >= date_add(@fromDate, interval 1 day) and s.tsstarted < @startOfCurrentWeek group by s.ACCOUNT_ID, yearweek(tsstarted, 5)) t
            where t.insider=1
            group by yearweek(lastdate, 5)
            order by t.lastDate desc;
    end if;
END;
#
drop procedure rptExtractFacebookNewRegistrations#
CREATE PROCEDURE `rptExtractFacebookNewRegistrations`()
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
            FROM strataprod.PLAYER p
                left join strataprod.LOBBY_USER u on p.user_id = u.user_id
            WHERE month(p.tscreated) <> 0 and u.provider_name = 'FACEBOOK' AND p.tscreated >= DATE(@fromDate)
            GROUP BY date(p.tscreated)
            ORDER BY p.tscreated desc;
    END IF;
END;
#

