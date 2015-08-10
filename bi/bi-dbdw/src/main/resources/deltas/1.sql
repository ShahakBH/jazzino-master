--
-- Table, procedure and event definitions dumped from production db when last strata.server.db delta was 263.sql
--

--
-- The place holder $MAINDB$ must be replaced with the main database name before this script is executed.
-- The procedure testForMainDBReplacement, checks that this replacement has happen, if not the script will fail.
drop procedure if exists testForMainDBReplacement#

--
-- Table structure for table `rpt_distinct_players`
--
CREATE TABLE `rpt_distinct_players` (
  `reportDate` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `slots` int(10) unsigned DEFAULT '0',
  `texas` int(10) unsigned DEFAULT '0',
  `roulette` int(10) unsigned DEFAULT '0',
  `blackjack` int(10) unsigned DEFAULT '0',
  `other` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`reportDate`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_distinct_players_monthly`
--
CREATE TABLE `rpt_distinct_players_monthly` (
  `monthEnding` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `slots` int(10) unsigned DEFAULT '0',
  `texas` int(10) unsigned DEFAULT '0',
  `roulette` int(10) unsigned DEFAULT '0',
  `blackjack` int(10) unsigned DEFAULT '0',
  `other` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`monthEnding`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_distinct_players_weekly`
--
CREATE TABLE `rpt_distinct_players_weekly` (
  `weekEnding` date NOT NULL,
  `players` int(10) unsigned DEFAULT '0',
  `slots` int(10) unsigned DEFAULT '0',
  `texas` int(10) unsigned DEFAULT '0',
  `roulette` int(10) unsigned DEFAULT '0',
  `blackjack` int(10) unsigned DEFAULT '0',
  `other` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`weekEnding`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_distinct_users_by_game_source`
--
CREATE TABLE `rpt_distinct_users_by_game_source` (
  `reportDate` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  `slotsUsers` int(10) unsigned DEFAULT '0',
  `texasUsers` int(10) unsigned DEFAULT '0',
  `rouletteUsers` int(10) unsigned DEFAULT '0',
  `blackjackUsers` int(10) unsigned DEFAULT '0',
  `otherUsers` int(10) unsigned DEFAULT '0',
  `complete` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`reportDate`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_distinct_users_by_game_source_weekly`
--
CREATE TABLE `rpt_distinct_users_by_game_source_weekly` (
  `weekEnding` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  `slotsUsers` int(10) unsigned DEFAULT '0',
  `texasUsers` int(10) unsigned DEFAULT '0',
  `rouletteUsers` int(10) unsigned DEFAULT '0',
  `blackjackUsers` int(10) unsigned DEFAULT '0',
  `otherUsers` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`weekEnding`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_distinct_users_by_game_source_monthly`
--
CREATE TABLE `rpt_distinct_users_by_game_source_monthly` (
  `monthEnding` date NOT NULL,
  `users` int(10) unsigned DEFAULT '0',
  `slotsUsers` int(10) unsigned DEFAULT '0',
  `texasUsers` int(10) unsigned DEFAULT '0',
  `rouletteUsers` int(10) unsigned DEFAULT '0',
  `blackjackUsers` int(10) unsigned DEFAULT '0',
  `otherUsers` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`monthEnding`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_external_transaction`
--
CREATE TABLE `rpt_external_transaction` (
  `reportDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `cashierName` varchar(255) NOT NULL,
  `results` bigint(21) NOT NULL DEFAULT '0',
  `amountUsd` decimal(65,4) DEFAULT NULL,
  `amountEur` decimal(65,4) DEFAULT NULL,
  `amountGbp` decimal(65,4) DEFAULT NULL,
  `complete` int(1) DEFAULT NULL
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_facebook_registration`
--
CREATE TABLE `rpt_facebook_registration` (
  `reportDate` timestamp NULL DEFAULT NULL,
  `registrations` bigint(21) NOT NULL DEFAULT '0',
  `complete` int(1) DEFAULT NULL
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_invitation`
--
CREATE TABLE `rpt_invitation` (
  `reportDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `invites` bigint(21) NOT NULL DEFAULT '0',
  `complete` int(1) DEFAULT NULL
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_registration`
--
CREATE TABLE `rpt_registration` (
  `reportDate` timestamp NULL DEFAULT NULL,
  `registrations` bigint(21) NOT NULL DEFAULT '0',
  `complete` int(1) DEFAULT NULL
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_report_status`
--
CREATE TABLE `rpt_report_status` (
  `report_action` varchar(45) NOT NULL,
  `action_ts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_action`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_tmp_account_activity`
--
CREATE TABLE `rpt_tmp_account_activity` (
  `account_id` int(11) NOT NULL,
  `audit_date` date NOT NULL,
  `slots` int(11) DEFAULT NULL,
  `texas_holdem` int(11) DEFAULT NULL,
  `blackjack` int(11) DEFAULT NULL,
  `roulette` int(11) DEFAULT NULL,
  `other` int(11) DEFAULT NULL,
  PRIMARY KEY (`account_id`,`audit_date`)
) ENGINE=InnoDB#

--
-- Table structure for table `rpt_tmp_table_activity`
--
CREATE TABLE `rpt_tmp_table_activity` (
  `account_id` int(11) NOT NULL,
  `audit_date` date NOT NULL,
  `table_id` int(11) NOT NULL,
  `slots` tinyint(1) DEFAULT '0',
  `texas_holdem` tinyint(1) DEFAULT '0',
  `blackjack` tinyint(1) DEFAULT '0',
  `roulette` tinyint(1) DEFAULT '0',
  `other` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`account_id`,`audit_date`,`table_id`)
) ENGINE=InnoDB#

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
    update rpt_tmp_table_activity a join TABLE_INFO ti on a.table_id = ti.table_id
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
              from strataprod.ACCOUNT_SESSION s left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
              where s.tsstarted >= @fromDate
              group by s.ACCOUNT_ID, date(s.tsstarted)) t
        where t.insider=1
        group by date(t.lastDate)
        order by t.lastDate desc;
END;
#
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
                  from strataprod.ACCOUNT_SESSION s left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where last_day(s.tsstarted) < last_day(curdate()) and s.tsstarted >= date_add(@fromDate, interval 1 day)
                  group by s.ACCOUNT_ID, DATE_FORMAT(s.tsstarted, '%m %Y')) t
            where t.insider=1
            group by DATE_FORMAT(lastDate, '%m %Y')
            order by t.lastDate desc;
    end if;
END;
#
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
                  from strataprod.ACCOUNT_SESSION s left join strataprod.PLAYER p on s.ACCOUNT_ID=p.ACCOUNT_ID
                  where s.tsstarted >= date_add(@fromDate, interval 1 day) and s.tsstarted < @startOfCurrentWeek group by s.ACCOUNT_ID, yearweek(tsstarted, 5)) t
            where t.insider=1
            group by yearweek(lastdate, 5)
            order by t.lastDate desc;
    end if;
END;
#
CREATE PROCEDURE `rptExtractExternalTransactions`()
BEGIN
    DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL;
    IF hour(now()) >= 2 THEN
        SELECT MIN(reportDate) INTO @lastIncompleteDate FROM rpt_external_transaction WHERE complete = 0;
        IF @lastIncompleteDate IS NOT NULL THEN
            set @fromDate = @lastIncompleteDate;
        ELSE
            SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate FROM rpt_external_transaction;
        END IF;
        DELETE FROM rpt_external_transaction where reportDate >= DATE(@fromDate);
        INSERT rpt_external_transaction(reportDate, cashierName, results, amountUsd, amountEur, amountGbp, complete)
            select p.message_timestamp, p.cashier_name, count(p.auto_id), sum((CURRENCY_CODE='USD')*p.AMOUNT),
                sum((CURRENCY_CODE='EUR')*p.AMOUNT) , sum((CURRENCY_CODE='GBP')*p.AMOUNT), p.message_timestamp < date(now())
            from strataprod.EXTERNAL_TRANSACTION p
            where month(p.message_timestamp) <> 0 and external_transaction_status='SUCCESS' and p.message_timestamp >= DATE(@fromDate)
            group by p.cashier_name, date(p.message_timestamp)
            order by p.message_timestamp desc;
    END IF;
END;
#
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
            FROM strataprod.PLAYER p left join strataprod.LOBBY_USER u on p.user_id = u.user_id
            WHERE month(p.tscreated) <> 0 and u.provider_name = 'FACEBOOK' AND p.tscreated >= DATE(@fromDate)
            GROUP BY date(p.tscreated)
            ORDER BY p.tscreated desc;
    END IF;
END;
#
CREATE PROCEDURE `rptExtractInvitations`()
BEGIN
    DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL;
    IF hour(now()) >= 2 THEN
        SELECT MIN(reportDate) INTO @lastIncompleteDate
        FROM rpt_invitation WHERE complete = 0;
        IF @lastIncompleteDate IS NOT NULL THEN
            set @fromDate = @lastIncompleteDate;
        ELSE
            SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate
            FROM rpt_invitation;
        END IF;
        DELETE FROM rpt_invitation where reportDate >= DATE(@fromDate);
        INSERT rpt_invitation(reportDate, invites, complete)
            select p.ts, count(p.id), p.ts < date(now())
            from strataprod.PLAYER_EVENT p
            where month(p.ts) <> 0 and p.TYPE LIKE '%Invite%' and p.ts >= DATE(@fromDate)
            group by date(p.ts)
            order by p.ts desc;
    END IF;
END;
#
CREATE PROCEDURE `rptExtractNewRegistrations`()
BEGIN
    DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL;
    IF hour(now()) >= 2 THEN
        SELECT MIN(reportDate) INTO @lastIncompleteDate FROM rpt_registration WHERE complete = 0;
        IF @lastIncompleteDate IS NOT NULL THEN
            set @fromDate = @lastIncompleteDate;
        ELSE
            SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate FROM rpt_registration;
        END IF;
        DELETE FROM rpt_registration where reportDate >= DATE(@fromDate);
        INSERT rpt_registration(reportDate, registrations, complete)
            SELECT tscreated, count(player_id), tscreated < date(now())
            FROM strataprod.PLAYER
            WHERE month(tscreated) <> 0 AND tscreated >= DATE(@fromDate)
            GROUP BY date(tscreated)
            ORDER BY tscreated desc;
    END IF;
END;
#
CREATE EVENT `rptExtractDistinctPlayers` ON SCHEDULE EVERY 1 HOUR STARTS '2010-10-13 09:22:52'  DO call rptExtractDistinctPlayers()#
CREATE EVENT `rptExtractDistinctUsersByGameSource` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-22 10:11:02' DO call rptExtractDistinctUsersByGameSource()#
CREATE EVENT `rptExtractDistinctUsersByGameSourceMonthly` ON SCHEDULE EVERY 1 DAY STARTS '2010-10-11 10:00:00' DO call rptExtractDistinctUsersByGameSourceMonthly()#
CREATE EVENT `rptExtractDistinctUsersByGameSourceWeekly` ON SCHEDULE EVERY 1 DAY STARTS '2010-10-11 10:00:00' DO call rptExtractDistinctUsersByGameSourceWeekly()#
CREATE EVENT `rptExtractExternalTransactions` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-15 10:29:40' DO call rptExtractExternalTransactions()#
CREATE EVENT `rptExtractFacebookNewRegistrations` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-15 10:29:20' DO call rptExtractFacebookNewRegistrations()#
CREATE EVENT `rptExtractInvitations` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-15 10:29:30' DO call rptExtractInvitations()#
CREATE EVENT `rptExtractNewRegistrations` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-15 10:29:10' DO call rptExtractNewRegistrations()#
