DROP PROCEDURE IF EXISTS extractAccountActivity#
CREATE PROCEDURE extractAccountActivity()
begin
	declare lastAuditCommandAutoId int;
	declare maxAuditCommandAutoId int;
	
	if get_lock('strataproddw.fill_account_activity_data', 0) = 1 then
	
		DROP TABLE IF EXISTS rpt_tmp_latest_commands;
		CREATE TEMPORARY TABLE rpt_tmp_latest_commands (
		  	`PLAYER_ID` BIGINT(21) NOT NULL ,
			`ACCOUNT_ID` BIGINT (21) NULL ,
	  		`AUDIT_DATE` DATE NULL ,
	  		`AUDIT_WEEK` DATE NULL ,
	  		`AUDIT_MONTH` DATE NULL ,
	  		`GAME_TYPE` VARCHAR(64) NULL ,
	  		PRIMARY KEY (`PLAYER_ID`),
	  		INDEX `PLAYERS` (`PLAYER_ID` ASC) );
	
		-- update player activity
	    select val into @lastAuditCommandAutoId from rpt_report_status where report_action = 'distinctPlayers';
	    IF @lastAuditCommandAutoId IS NULL THEN
	    	SET @lastAuditCommandAutoId = 0;
	    END IF;
	    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;
	
		insert into rpt_tmp_latest_commands(PLAYER_ID,AUDIT_DATE,AUDIT_WEEK,AUDIT_MONTH,GAME_TYPE)
			select distinct account_id, date(audit_ts), 
				last_day_of_week(audit_ts),last_day(audit_ts),game_type
		        from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
		        where auto_id > @lastAuditCommandAutoId and auto_id <= @maxAuditCommandAutoId
		          and command_type not in ('Leave', 'GetStatus');
		          
		UPDATE rpt_tmp_latest_commands c
		JOIN strataprod.PLAYER p ON p.PLAYER_ID=c.PLAYER_ID
		SET c.ACCOUNT_ID=p.ACCOUNT_ID;

	    DELETE FROM rpt_recent_registrations WHERE AUDIT_TIME < now() - INTERVAL 98 HOUR;
	
		UPDATE rpt_recent_registrations
		SET PLAYED = IF(AUDIT_TIME > now() - INTERVAL 1 DAY,1,2)
		WHERE ACCOUNT_ID IN
			(SELECT DISTINCT ACCOUNT_ID FROM rpt_tmp_latest_commands);
	
		insert ignore into rpt_account_activity (player_id, audit_date, game_type)
			(SELECT DISTINCT PLAYER_ID, AUDIT_DATE, GAME_TYPE FROM rpt_tmp_latest_commands);
	
	    insert ignore into rpt_account_activity_weekly (player_id, audit_date, game_type)
	    	(SELECT DISTINCT PLAYER_ID, AUDIT_WEEK, GAME_TYPE FROM rpt_tmp_latest_commands);
	
	    insert ignore into rpt_account_activity_monthly (player_id, audit_date, game_type)
	    	(SELECT DISTINCT PLAYER_ID, AUDIT_MONTH, GAME_TYPE FROM rpt_tmp_latest_commands);
	
	    insert ignore into rpt_account_activity (player_id, audit_date, game_type)
	    	(SELECT DISTINCT PLAYER_ID, AUDIT_DATE, '' FROM rpt_tmp_latest_commands);
	
	    insert ignore into rpt_account_activity_weekly (player_id, audit_date, game_type)
	    	(SELECT DISTINCT PLAYER_ID, AUDIT_WEEK, '' FROM rpt_tmp_latest_commands);
	
	    insert ignore into rpt_account_activity_monthly (player_id, audit_date, game_type)
	    	(SELECT DISTINCT PLAYER_ID, AUDIT_MONTH, '' FROM rpt_tmp_latest_commands);
	
		update rpt_report_status set action_ts = now(), val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';
		
		do release_lock('strataproddw.fill_account_activity_data');
	end if;
end;
#
