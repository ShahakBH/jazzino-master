DROP PROCEDURE IF EXISTS extractAccountActivity#
CREATE PROCEDURE extractAccountActivity()
begin
	declare lastAuditCommandAutoId int;
	declare maxAuditCommandAutoId int;

    -- update player activity
    select val into @lastAuditCommandAutoId from rpt_report_status where report_action = 'distinctPlayers';
    IF @lastAuditCommandAutoId IS NULL THEN
    	SET @lastAuditCommandAutoId = 0;
    END IF;
    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;

    DELETE FROM rpt_recent_registrations WHERE AUDIT_TIME < now() - INTERVAL 98 HOUR;

	UPDATE rpt_recent_registrations
	SET PLAYED = IF(AUDIT_TIME > now() - INTERVAL 1 DAY,1,2)
	WHERE ACCOUNT_ID IN
	(select p.account_id
    from (
        select account_id
        from AUDIT_COMMAND
        where auto_id > @lastAuditCommandAutoId
        and auto_id <= @maxAuditCommandAutoId
        and command_type not in ('Leave', 'GetStatus')) ac
    join strataprod.PLAYER p
    on p.player_id = ac.account_id);

	insert ignore into rpt_account_activity (player_id, audit_date, game_type)
      select distinct account_id, date(audit_ts), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

    insert ignore into rpt_account_activity_weekly (player_id, audit_date, game_type)
      select distinct account_id, last_day_of_week(audit_ts), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

    insert ignore into rpt_account_activity_monthly (player_id, audit_date, game_type)
      select distinct account_id, last_day(audit_ts), game_type
        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

    insert ignore into rpt_account_activity (player_id, audit_date, game_type)
      select distinct account_id, date(audit_ts), ''
        from AUDIT_COMMAND ac
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

    insert ignore into rpt_account_activity_weekly (player_id, audit_date, game_type)
      select distinct account_id, last_day_of_week(audit_ts), ''
        from AUDIT_COMMAND ac
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

    insert ignore into rpt_account_activity_monthly (player_id, audit_date, game_type)
      select distinct account_id, last_day(audit_ts), ''
        from AUDIT_COMMAND ac
        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
          and command_type not in ('Leave', 'GetStatus');

	update rpt_report_status set action_ts = now(), val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';
end;
#
