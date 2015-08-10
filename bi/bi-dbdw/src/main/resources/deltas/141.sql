DROP TRIGGER IF EXISTS invites_by_player_trigger#
CREATE TRIGGER invites_by_player_trigger
AFTER INSERT ON INVITATIONS
FOR EACH ROW
BEGIN
	DECLARE source_val varchar(255) DEFAULT '';
	DECLARE creation_date_val date DEFAULT NULL;
	
	SELECT s.SOURCE,DATE(p.TSCREATED) 
		into source_val,creation_date_val
		FROM PLAYER_DEFINITION p
		LEFT JOIN rpt_account_sources_mv s USING (ACCOUNT_ID)
		WHERE p.PLAYER_ID = NEW.PLAYER_ID;
	
	INSERT INTO rpt_invites_by_player_and_day(PLAYER_ID,SOURCE,AUDIT_DATE,CREATION_DATE,TOTAL_SENT)
		VALUES(NEW.PLAYER_ID,IFNULL(source_val,'Natural'),DATE(NEW.CREATED_TS),creation_date_val,1)
	ON DUPLICATE KEY UPDATE TOTAL_SENT = TOTAL_SENT + 1;
		
	IF NEW.STATUS = 'ACCEPTED' THEN
		CALL count_accepted_invite(NEW.PLAYER_ID,DATE(NEW.CREATED_TS),DATE(NEW.CREATED_TS));
	END IF;
END
#

DROP TRIGGER IF EXISTS invites_by_player_update_trigger#
CREATE TRIGGER invites_by_player_update_trigger
AFTER UPDATE ON INVITATIONS
FOR EACH ROW
BEGIN
	IF OLD.STATUS<>'ACCEPTED' AND NEW.STATUS = 'ACCEPTED' AND NOT (NEW.UPDATED_TS IS NULL) THEN
		CALL count_accepted_invite(NEW.PLAYER_ID,DATE(NEW.UPDATED_TS),DATE(NEW.CREATED_TS));
	END IF;
END
#

DROP TRIGGER IF EXISTS players_by_week_trigger#
CREATE TRIGGER players_by_week_trigger
AFTER INSERT ON rpt_players_by_week
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
 
  IF NEW.GAME_TYPE = '' THEN
	  SELECT SOURCE into source_val
	  FROM rpt_account_sources_mv
	  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
		
	  INSERT INTO rpt_users_by_week_source_count(AUDIT_DATE,PLAYERS,SOURCE)
	  	VALUES(NEW.AUDIT_DATE,1,IFNULL(source_val,'Natural'))
	  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
  END IF;
END
#

DROP TRIGGER IF EXISTS users_by_month_trigger#
CREATE TRIGGER users_by_month_trigger
AFTER INSERT ON rpt_users_by_month
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
  
  SELECT SOURCE into source_val
  FROM rpt_account_sources_mv
  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
	
  INSERT INTO rpt_users_by_month_source_count(AUDIT_DATE,USERS,SOURCE)
  	VALUES(NEW.AUDIT_DATE,1,IFNULL(source_val,'Natural'))
  	ON DUPLICATE KEY
  	UPDATE USERS = USERS+1;
END
#

DROP TRIGGER IF EXISTS players_by_month_trigger#
CREATE TRIGGER players_by_month_trigger
AFTER INSERT ON rpt_players_by_month
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
 
  IF NEW.GAME_TYPE = '' THEN
	  SELECT SOURCE into source_val
	  FROM rpt_account_sources_mv
	  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
		
	  INSERT INTO rpt_users_by_month_source_count(AUDIT_DATE,PLAYERS,SOURCE)
	  	VALUES(NEW.AUDIT_DATE,1,IFNULL(source_val,'Natural'))
	  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
  END IF;
END
#


DROP FUNCTION IF EXISTS date_hour#
CREATE FUNCTION date_hour(dt datetime) RETURNS DATETIME
 	RETURN dt - INTERVAL MINUTE(dt) MINUTE - INTERVAL SECOND(dt) SECOND - INTERVAL MICROSECOND(dt) MICROSECOND#

CREATE TABLE IF NOT EXISTS `rpt_players_by_platform_and_time` (
  ACCOUNT_ID bigint(21) NOT NULL,
  PERIOD CHAR(4) NOT NULL,
  AUDIT_DATE datetime DEFAULT NULL,
  GAME_TYPE varchar(32) NOT NULL,
  PLATFORM varchar(64),
  PRIMARY KEY (ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
)#

CREATE TABLE IF NOT EXISTS `rpt_players_by_platform_and_time_count` (
  PERIOD CHAR(4) NOT NULL,
  AUDIT_DATE datetime DEFAULT NULL,
  GAME_TYPE varchar(32) NOT NULL,
  SOURCE varchar(255) DEFAULT NULL,
  PLATFORM varchar(64),
  PLAYERS int(11),
  PRIMARY KEY (PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM,SOURCE)
)#

DROP PROCEDURE IF EXISTS createTmpLatestCommands#
DROP PROCEDURE IF EXISTS createAndFillTmpLatestCommands#
CREATE PROCEDURE createAndFillTmpLatestCommands(IN lastAuditCommandAutoId bigint(20), IN maxAuditCommandAutoId bigint(20))
BEGIN
	DROP TABLE IF EXISTS rpt_tmp_latest_commands;
		CREATE TEMPORARY TABLE rpt_tmp_latest_commands (
		  	`PLAYER_ID` BIGINT(21) NOT NULL ,
			`ACCOUNT_ID` BIGINT (21) NULL ,
	  		`AUDIT_DATE_HOUR` DATETIME NULL ,
	  		`AUDIT_DATE` DATE NULL ,
	  		`AUDIT_WEEK` DATE NULL ,
	  		`AUDIT_MONTH` DATE NULL ,
	  		`GAME_TYPE` VARCHAR(64) NULL ,
	  		`PLATFORM` VARCHAR(32) NULL ,
	  		PRIMARY KEY (`PLAYER_ID`),
	  		INDEX `ACCOUNTS` (`ACCOUNT_ID` ASC) );
	  		
	  insert ignore into rpt_tmp_latest_commands(PLAYER_ID,AUDIT_DATE_HOUR,AUDIT_DATE,AUDIT_WEEK,AUDIT_MONTH,GAME_TYPE,ACCOUNT_ID,PLATFORM)
			select distinct ac.account_id, date_hour(audit_ts), date(audit_ts),
				last_day_of_week(audit_ts),last_day(audit_ts),game_type,
				p.ACCOUNT_ID,psc.SESSION_PLATFORM
		        from AUDIT_COMMAND ac join strataproddw.TABLE_INFO ti on ac.table_id = ti.table_id
		        LEFT JOIN PLAYER_DEFINITION p ON ac.account_id = p.PLAYER_ID
		        LEFT JOIN PLAYER_STATUS_CURRENT psc ON p.ACCOUNT_ID = psc.ACCOUNT_ID
		        where auto_id > lastAuditCommandAutoId and auto_id <= maxAuditCommandAutoId
		          and command_type not in ('Leave', 'GetStatus');
END#

DROP PROCEDURE IF EXISTS extractSessionState#
CREATE PROCEDURE extractSessionState(IN actual_time DATETIME)
BEGIN
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'hour', tlc.AUDIT_DATE_HOUR, tlc.GAME_TYPE, tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'hour', tlc.AUDIT_DATE_HOUR, tlc.GAME_TYPE, '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'hour', tlc.AUDIT_DATE_HOUR, '*', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'hour', tlc.AUDIT_DATE_HOUR, '*', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
	
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, tlc.GAME_TYPE, tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, tlc.GAME_TYPE, '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, '*', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, '*', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, '#', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'day', tlc.AUDIT_DATE, '#', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
			
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, tlc.GAME_TYPE, tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, tlc.GAME_TYPE, '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, '*', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, '*', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, '#', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'week', tlc.AUDIT_WEEK, '#', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
			
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, tlc.GAME_TYPE, tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, tlc.GAME_TYPE, '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, '*', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, '*', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, '#', '*' 
			FROM rpt_tmp_latest_commands tlc);
		INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
			(SELECT DISTINCT tlc.ACCOUNT_ID, 'mon', tlc.AUDIT_MONTH, '#', tlc.PLATFORM 
			FROM rpt_tmp_latest_commands tlc);
		
		INSERT INTO 
			PLAYER_STATUS_CURRENT(ACCOUNT_ID, SESSION_PLATFORM, LOGIN_GAME_LOBBY, LOGIN_TS, LAST_ACTIVITY_TS, SESSION_ID, ACTUAL_GAME, NEW_GAME, GAME_START_TS)
			SELECT d.ACCOUNT_ID,'','',actual_time,actual_time,0,c.GAME_TYPE,c.GAME_TYPE,IF(c.GAME_TYPE<>'NONE',actual_time,NULL) 
			FROM PLAYER_DEFINITION d
			JOIN rpt_tmp_latest_commands c USING(PLAYER_ID)
		ON DUPLICATE KEY UPDATE
			NEW_GAME=c.GAME_TYPE, LAST_ACTIVITY_TS = actual_time;
		
		INSERT INTO PLAYER_GAME_SESSION(ACCOUNT_ID,SESSION_ID,GAME_TYPE,SESSION_LENGTH_SECS,END_TS)
			SELECT ACCOUNT_ID,SESSION_ID,ACTUAL_GAME,
				TIMESTAMPDIFF(SECOND,GAME_START_TS,LAST_ACTIVITY_TS),LAST_ACTIVITY_TS
			FROM PLAYER_STATUS_CURRENT
			WHERE
				ACTUAL_GAME <> 'NONE' AND
				(LAST_ACTIVITY_TS < actual_time - INTERVAL 30 MINUTE OR  ACTUAL_GAME <> NEW_GAME);
				
		REPLACE INTO PLAYER_CLOSED_SESSION(ACCOUNT_ID,SESSION_ID,SESSION_LENGTH_SECS,END_TS,SESSION_PLATFORM,LOGIN_GAME_LOBBY)
			SELECT ACCOUNT_ID,SESSION_ID,
				IF(LOGIN_TS < LAST_ACTIVITY_TS,TIMESTAMPDIFF(SECOND,LOGIN_TS,LAST_ACTIVITY_TS),0),
				LAST_ACTIVITY_TS,SESSION_PLATFORM,LOGIN_GAME_LOBBY
			FROM PLAYER_STATUS_CURRENT
			WHERE 
				LAST_ACTIVITY_TS < actual_time - INTERVAL 30 MINUTE;
			
		UPDATE PLAYER_STATUS_CURRENT
			SET GAME_START_TS=actual_time,ACTUAL_GAME=NEW_GAME
			WHERE ACTUAL_GAME <> NEW_GAME;
			
		DELETE FROM PLAYER_STATUS_CURRENT WHERE LAST_ACTIVITY_TS < actual_time - INTERVAL 300 MINUTE;
END#

DROP TRIGGER IF EXISTS players_by_platform_and_time_trigger#
CREATE TRIGGER players_by_platform_and_time_trigger
AFTER INSERT ON rpt_players_by_platform_and_time
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
  
  SELECT s.SOURCE into source_val
		FROM rpt_account_sources_mv s
		WHERE s.ACCOUNT_ID = NEW.ACCOUNT_ID;

  INSERT INTO rpt_players_by_platform_and_time_count(PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM,PLAYERS,SOURCE)
  	VALUES(NEW.PERIOD,NEW.AUDIT_DATE,NEW.GAME_TYPE,
  		IF(NEW.PLATFORM = '*' , '*', IF(NEW.PLATFORM = 'MOBILE','iOS',IF(NEW.PLATFORM = 'ANDROID', 'Android', 'Web'))),1,IFNULL(source_val,'Natural'))
  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
END
#

DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE account_inserts(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64), IN start_page_val VARCHAR(2048))
BEGIN
    DECLARE user_id_val BIGINT(20) DEFAULT 0;
    declare maxUserId BIGINT(20) DEFAULT 0;

    SELECT MAX(USER_ID) INTO @maxUserId FROM rpt_recent_registrations;

    IF NOT @maxUserId IS NULL THEN
        INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
        SELECT lu.USER_ID,lu.TSREG,lu.PROVIDER_NAME,IF(lu.PROVIDER_NAME='YAZINO',lu.USER_ID,lu.EXTERNAL_ID),pd.ACCOUNT_ID,lu.FIRST_NAME
            FROM LOBBY_USER lu
            JOIN PLAYER_DEFINITION pd USING (PLAYER_ID)
            WHERE lu.USER_ID > @maxUserId;
     ELSE
         INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
         SELECT lu.USER_ID,lu.TSREG,lu.PROVIDER_NAME,IF(lu.PROVIDER_NAME='YAZINO',lu.USER_ID,lu.EXTERNAL_ID),pd.ACCOUNT_ID,lu.FIRST_NAME
             FROM LOBBY_USER lu
             JOIN PLAYER_DEFINITION pd USING (PLAYER_ID)
             WHERE lu.TSREG > now() - INTERVAL 98 HOUR;
    END IF;

   SELECT `p`.`USER_ID`
   INTO @user_id_val
   FROM
     strataproddw.PLAYER `p`
   WHERE
     ACCOUNT_ID = account_id_val;
     
   UPDATE rpt_recent_registrations SET PLATFORM = platform_val, START_PAGE = start_page_val WHERE USER_ID = @user_id_val AND PLATFORM = '';

     INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, REGISTRATION_PLATFORM)
      VALUES (account_id_val,platform_val)
      ON DUPLICATE KEY
      UPDATE REGISTRATION_PLATFORM =
        IF(REGISTRATION_PLATFORM IS NULL,VALUES(REGISTRATION_PLATFORM),REGISTRATION_PLATFORM);
        
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'day',DATE(tsstarted_val),'#','*');
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'day',DATE(tsstarted_val),'#',platform_val);
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'week',last_day_of_week(tsstarted_val),'#','*');
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'week',last_day_of_week(tsstarted_val),'#',platform_val);
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'mon',last_day(tsstarted_val),'#','*');
     INSERT IGNORE INTO rpt_players_by_platform_and_time(ACCOUNT_ID,PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM)
     	VALUES(account_id_val,'mon',last_day(tsstarted_val),'#',platform_val);
END
#

DROP PROCEDURE IF EXISTS extractAccountActivityAt#
CREATE PROCEDURE extractAccountActivityAt(IN actual_time DATETIME)
begin
	declare lastAuditCommandAutoId bigint(20);
	declare maxAuditCommandAutoId bigint(20);

	if get_lock('strataproddw.fill_account_activity_data_f', 0) = 1 then
	
		select val into @lastAuditCommandAutoId from rpt_report_status where report_action = 'distinctPlayers';
	    IF @lastAuditCommandAutoId IS NULL THEN
	    	SET @lastAuditCommandAutoId = 0;
	    END IF;
	    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;

	    CALL createAndFillTmpLatestCommands(@lastAuditCommandAutoId, @maxAuditCommandAutoId);
		
	    DELETE FROM rpt_recent_registrations WHERE AUDIT_TIME < actual_time - INTERVAL 98 HOUR;

		UPDATE rpt_recent_registrations
		SET PLAYED = IF(AUDIT_TIME > actual_time - INTERVAL 1 DAY,1,2)
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

		update rpt_report_status set action_ts = actual_time, val = @maxAuditCommandAutoId where report_action = 'distinctPlayers';
		
		CALL extractSessionState(actual_time);

		do release_lock('strataproddw.fill_account_activity_data_f');
	end if;
end;
#