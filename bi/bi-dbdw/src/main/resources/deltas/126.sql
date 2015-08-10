CREATE TABLE IF NOT EXISTS `PLAYER_STATUS_CURRENT` (
  `ACCOUNT_ID` bigint(21) NOT NULL,
  `SESSION_PLATFORM` varchar(32) DEFAULT NULL,
  `LOGIN_GAME_LOBBY` varchar(32) DEFAULT NULL,
  `SESSION_ID` bigint(21) DEFAULT NULL,
  `LOGIN_TS` datetime DEFAULT NULL,
  `GAME_START_TS` datetime DEFAULT NULL,
  `LAST_ACTIVITY_TS` datetime DEFAULT NULL,
  `ACTUAL_GAME` varchar(32) DEFAULT 'NONE',
  `NEW_GAME` varchar(32) DEFAULT 'NONE',
  IP_ADDRESS bigint(21) NULL,
  IP_HOB int(11) NULL,
  PRIMARY KEY (`ACCOUNT_ID`),
  KEY `SESSION_PLATFORM` (`SESSION_PLATFORM`),
  KEY `LOGIN_GAME_LOBBY` (`LOGIN_GAME_LOBBY`),
  KEY `ACTUAL_GAME` (`ACTUAL_GAME`),
  KEY `NEW_GAME` (`NEW_GAME`),
  KEY LAST_ACTIVITY_TS (LAST_ACTIVITY_TS),
  KEY SESSION_ID (SESSION_ID)
)#

CREATE TABLE IF NOT EXISTS PLAYER_GAME_SESSION (
	AUTO_ID bigint(21) NOT NULL AUTO_INCREMENT,
	ACCOUNT_ID bigint(21) unsigned NOT NULL,
	SESSION_ID bigint(21) NOT NULL,
	GAME_TYPE varchar(32) NOT NULL,
	SESSION_LENGTH_SECS int(21) NOT NULL,
	END_TS DATETIME,
	PRIMARY KEY (AUTO_ID),
	KEY GAME_TYPE (GAME_TYPE),
	KEY ACCOUNT_ID (ACCOUNT_ID),
	KEY END_TS (END_TS)
)#

CREATE TABLE IF NOT EXISTS PLAYER_CLOSED_SESSION (
	SESSION_ID bigint(21) NOT NULL,
	ACCOUNT_ID bigint(21) unsigned NOT NULL,
	SESSION_LENGTH_SECS int(21) NOT NULL,
	SESSION_PLATFORM varchar(32) DEFAULT NULL,
	LOGIN_GAME_LOBBY varchar(32) DEFAULT NULL,
	END_TS DATETIME,
    IP_ADDRESS bigint(21) NULL,
    IP_HOB int(11) NULL,
	PRIMARY KEY (SESSION_ID, ACCOUNT_ID),
	KEY ACCOUNT_ID (ACCOUNT_ID),
	KEY END_TS (END_TS),
	KEY SESSION_PLATFORM (SESSION_PLATFORM),
	KEY LOGIN_GAME_LOBBY (LOGIN_GAME_LOBBY)
)#

CREATE TABLE IF NOT EXISTS `GEOLOC_RANGES` (
  `START_IP` int(11) unsigned NOT NULL,
  `END_IP` int(11) unsigned NOT NULL,
  `LOCATION_ID` bigint(21) NOT NULL,
  `HIGH_ORDER_BYTE` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`START_IP`,`END_IP`),
  KEY `HIGH_ORDER_BYTE` (`HIGH_ORDER_BYTE`)
)#

CREATE TABLE IF NOT EXISTS `GEOLOC_SESSIONS` (
  `SESSION_ID` bigint(21) NOT NULL,
  `LOCATION_ID` bigint(21) NOT NULL,
  IP_ADDRESS bigint(21) NULL,
  PRIMARY KEY (SESSION_ID),
  KEY IP_ADDRESS (IP_ADDRESS)
)#

CREATE TABLE IF NOT EXISTS `GEOLOC_ACCOUNTS` (
  `ACCOUNT_ID` int(11) NOT NULL,
  `LOCATION_ID` bigint(21) NOT NULL,
  IP_ADDRESS bigint(21) NULL,
  PRIMARY KEY (ACCOUNT_ID)
)#

CREATE TABLE IF NOT EXISTS `GEOLOC_LOCATIONS` (
  LOCATION_ID bigint(21) NOT NULL,
  COUNTRY char(4) NOT NULL DEFAULT '',
  REGION varchar(16) NOT NULL DEFAULT '',
  CITY varchar(64) NOT NULL DEFAULT '',
  POSTAL_CODE varchar(16) NOT NULL DEFAULT '',
  LATITUDE decimal(16,8) NOT NULL DEFAULT 0,
  LONGITUDE decimal(16,8) NOT NULL DEFAULT 0,
  METRO_CODE varchar(16) NOT NULL DEFAULT '',
  AREA_CODE varchar(16) NOT NULL DEFAULT '',
  PRIMARY KEY (LOCATION_ID)
)#

DROP FUNCTION IF EXISTS toInt#
CREATE FUNCTION toInt(arg VARCHAR(32)) 
RETURNS INT DETERMINISTIC
BEGIN
	RETURN CONVERT(CONVERT(arg USING utf8),SIGNED);
END;
#

DROP FUNCTION IF EXISTS ipToNum#
CREATE FUNCTION ipToNum(s VARCHAR(64)) 
RETURNS VARCHAR(32)
BEGIN
	DECLARE remains VARCHAR(32);
	DECLARE result BIGINT(20) DEFAULT 0;
	DECLARE pos2 INT;
	
	SET pos2 = INSTR(s,'.');
	IF pos2 = 0 THEN RETURN 0; END IF;
	SET result = toInt(SUBSTRING(s,1,pos2-1)) * 16777216;
	SET remains = SUBSTRING(s, pos2 + 1);
	
	SET pos2 = INSTR(remains,'.');
	IF pos2 = 0 THEN RETURN 0; END IF;
	SET result = result + toInt(SUBSTRING(remains,1,pos2-1)) * 65536;
	SET remains = SUBSTRING(remains, pos2 + 1);
	
	SET pos2 = INSTR(remains,'.');
	IF pos2 = 0 THEN RETURN 0; END IF;
	SET result = result + toInt(SUBSTRING(remains,1,pos2-1)) * 256;
	SET remains = SUBSTRING(remains, pos2 + 1);
	
	SET result = result + toInt(remains);
	
	RETURN result;
	
END;
#

DROP FUNCTION IF EXISTS highOrderByte#

DROP PROCEDURE IF EXISTS extractLocations#
CREATE PROCEDURE extractLocations()
BEGIN
	DECLARE from_id bigint(21);
	
	if get_lock('strataproddw.extract_geolocations', 0) = 1 then
	
		SELECT MAX(SESSION_ID) INTO from_id FROM GEOLOC_SESSIONS;
		IF NOT from_id IS NULL THEN
			CALL extractLocationsFrom(from_id);
		END IF;
		
		do release_lock('strataproddw.extract_geolocations');
	end if;
END;
#

DROP PROCEDURE IF EXISTS extractLocationInfo#
CREATE PROCEDURE extractLocationInfo(IN account_id_val int(11), IN session_id_val bigint(21), IN ip_val VARCHAR(32))
BEGIN
	DECLARE location_id_val BIGINT(21) DEFAULT NULL;
	DECLARE ip_num BIGINT(21);
	
	IF NOT ip_val IS NULL THEN
		SET ip_num = ipToNum(ip_val);
		
		IF ip_num > 0 THEN
			SELECT LOCATION_ID 
				INTO location_id_val
				FROM GEOLOC_RANGES 
				WHERE ip_num >= START_IP
				ORDER BY START_IP DESC
				LIMIT 1;
					
			IF NOT location_id_val IS NULL THEN
				REPLACE INTO GEOLOC_SESSIONS(SESSION_ID,LOCATION_ID,IP_ADDRESS)
					VALUES(session_id_val,location_id_val,ip_num);
				REPLACE INTO GEOLOC_ACCOUNTS(ACCOUNT_ID,LOCATION_ID,IP_ADDRESS)
					VALUES(account_id_val,location_id_val,ip_num);
			END IF;	
		END IF;
	END IF;
END#

DROP PROCEDURE IF EXISTS extractLocationsFrom#
CREATE PROCEDURE extractLocationsFrom(IN from_id BIGINT(21))
BEGIN
	DECLARE done BOOLEAN DEFAULT FALSE;
	DECLARE account_id_val int(11);
	DECLARE session_id_val bigint(21);
	DECLARE ip_val VARCHAR(32) DEFAULT NULL;
	
	DECLARE session_cursor CURSOR FOR
	SELECT ACCOUNT_ID,SESSION_ID,IP_ADDRESS
		FROM ACCOUNT_SESSION
		WHERE SESSION_ID > from_id
		ORDER BY SESSION_ID;

	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
	
  	OPEN session_cursor;
	session_loop: LOOP
		SET done = false;
	    FETCH session_cursor INTO account_id_val, session_id_val,ip_val;
	    IF done THEN LEAVE session_loop; END IF;
    	
		CALL extractLocationInfo(account_id_val,session_id_val,ip_val);
	    SET ip_val = NULL;
	END LOOP session_loop;
  	CLOSE session_cursor;
END#

DROP TRIGGER IF EXISTS account_actions#

DROP PROCEDURE IF EXISTS player_status_session_update#
CREATE PROCEDURE player_status_session_update(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64), 
	IN start_page_val VARCHAR(2048),IN session_id_val BIGINT(20))
BEGIN 
	REPLACE INTO PLAYER_STATUS_CURRENT(ACCOUNT_ID, SESSION_PLATFORM, LOGIN_GAME_LOBBY, LOGIN_TS, LAST_ACTIVITY_TS, SESSION_ID)
		VALUES(account_id_val, platform_val, 
		IF(
			INSTR(start_page_val,'WHEELDEAL')>0 OR INSTR(start_page_val,'SLOTS')>0, 'SLOTS',
			IF(
				INSTR(start_page_val,'ROULETTE')>0 OR INSTR(start_page_val,'HOMEROULETTE')>0, 'ROULETTE',
				IF(
					INSTR(start_page_val,'BLACKJACK')>0 OR INSTR(start_page_val,'HOMEBLACKJACK')>0, 'BLACKJACK',
					IF(
						INSTR(start_page_val,'TEXASHOLDEM')>0 OR INSTR(start_page_val,'HOMETEXASHOLDEM')>0 
							OR INSTR(start_page_val,'TEXAS_HOLDEM')>0 OR INSTR(start_page_val,'POKER')>0, 'TEXAS_HOLDEM',
						IF(
							INSTR(start_page_val,'HIGHSTAKES')>0 OR INSTR(start_page_val,'HIGH_STAKES')>0, 'HIGH_STAKES',
							IF(
								INSTR(start_page_val,'HISSTERIA')>0 OR INSTR(start_page_val,'HOMEHISSTERIA')>0, 'HISSTERIA',
								IF(
									INSTR(start_page_val,'BINGO')>0 OR INSTR(start_page_val,'EXTREMEBINGO')>0
										 OR INSTR(start_page_val,'EXTREME_BINGO')>0 OR INSTR(start_page_val,'HOMEEXTREMEBINGO')>0, 'BINGO',
									IF(
										INSTR(start_page_val,'lobby')>0 OR INSTR(start_page_val,'messages')>0 OR 
										INSTR(start_page_val,'dailyAward')>0 OR INSTR(start_page_val,'tournaments')>0 OR 
										INSTR(start_page_val,'error')>0 OR 
										INSTR(start_page_val,'LOGIN')>0 OR start_page_val = '', 'NOT_REPORTED',
										'UNKNOWN'
									)
								)
							)
						)
					)
				)
			)
		), tsstarted_val, tsstarted_val, session_id_val);
END#

CREATE TRIGGER account_actions
AFTER INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
  CALL account_inserts(NEW.ACCOUNT_ID,NEW.TSSTARTED,NEW.PLATFORM,NEW.START_PAGE);
  CALL player_status_session_update(NEW.ACCOUNT_ID,NEW.TSSTARTED,NEW.PLATFORM,NEW.START_PAGE,NEW.SESSION_ID);
  CALL extractLocationInfo(NEW.ACCOUNT_ID,NEW.SESSION_ID,NEW.IP_ADDRESS);
END
#

DROP PROCEDURE IF EXISTS extractAccountActivity#
CREATE PROCEDURE extractAccountActivity()
BEGIN
	call extractAccountActivityAt(now());
END#

DROP PROCEDURE IF EXISTS createTmpLatestCommands#
DROP PROCEDURE IF EXISTS createAndFillTmpLatestCommands#
CREATE PROCEDURE createAndFillTmpLatestCommands(IN lastAuditCommandAutoId bigint(20), IN maxAuditCommandAutoId bigint(20))
BEGIN
	DROP TABLE IF EXISTS rpt_tmp_latest_commands;
		CREATE TEMPORARY TABLE rpt_tmp_latest_commands (
		  	`PLAYER_ID` BIGINT(21) NOT NULL ,
			`ACCOUNT_ID` BIGINT (21) NULL ,
	  		`AUDIT_DATE` DATE NULL ,
	  		`AUDIT_WEEK` DATE NULL ,
	  		`AUDIT_MONTH` DATE NULL ,
	  		`GAME_TYPE` VARCHAR(64) NULL ,
	  		PRIMARY KEY (`PLAYER_ID`),
	  		INDEX `ACCOUNTS` (`ACCOUNT_ID` ASC) );
	  		
	  insert ignore into rpt_tmp_latest_commands(PLAYER_ID,AUDIT_DATE,AUDIT_WEEK,AUDIT_MONTH,GAME_TYPE)
			select distinct account_id, date(audit_ts),
				last_day_of_week(audit_ts),last_day(audit_ts),game_type
		        from AUDIT_COMMAND ac join strataproddw.TABLE_INFO ti on ac.table_id = ti.table_id
		        where auto_id > lastAuditCommandAutoId and auto_id <= maxAuditCommandAutoId
		          and command_type not in ('Leave', 'GetStatus');

		UPDATE rpt_tmp_latest_commands c
		JOIN PLAYER_DEFINITION p USING(PLAYER_ID)
		SET c.ACCOUNT_ID=p.ACCOUNT_ID;
END#

DROP PROCEDURE IF EXISTS extractSessionState#
CREATE PROCEDURE extractSessionState(IN actual_time DATETIME)
BEGIN
		INSERT INTO 
			PLAYER_STATUS_CURRENT(ACCOUNT_ID, SESSION_PLATFORM, LOGIN_GAME_LOBBY, LOGIN_TS, LAST_ACTIVITY_TS, SESSION_ID, ACTUAL_GAME, NEW_GAME)
			SELECT d.ACCOUNT_ID,'','',actual_time,actual_time,0,c.GAME_TYPE,c.GAME_TYPE FROM PLAYER_DEFINITION d
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

DROP PROCEDURE IF EXISTS extractAccountActivityAt#
CREATE PROCEDURE extractAccountActivityAt(IN actual_time DATETIME)
begin
	declare lastAuditCommandAutoId bigint(20);
	declare maxAuditCommandAutoId bigint(20);

	if get_lock('strataproddw.fill_account_activity_data_d', 0) = 1 then
	
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

		do release_lock('strataproddw.fill_account_activity_data_d');
	end if;
end;
#

DROP VIEW IF EXISTS bi_players_by_source#
DROP VIEW IF EXISTS bi_players_by_date_and_source#
CREATE VIEW bi_players_by_date_and_source AS
	SELECT DATE(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, ps.SOURCE AS SOURCE
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY DATE(pcs.END_TS),ps.SOURCE#

DROP VIEW IF EXISTS bi_players_by_week_and_source#
CREATE VIEW bi_players_by_week_and_source AS
	SELECT last_day_of_week(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, ps.SOURCE AS SOURCE
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEARWEEK(pcs.END_TS,5),ps.SOURCE#


DROP VIEW IF EXISTS bi_players_by_month_and_source#
CREATE VIEW bi_players_by_month_and_source AS
	SELECT last_day(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, ps.SOURCE AS SOURCE
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEAR(pcs.END_TS),MONTH(pcs.END_TS),ps.SOURCE#

DROP VIEW IF EXISTS bi_players_by_source#
CREATE VIEW bi_players_by_source AS
	SELECT COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, ps.SOURCE AS SOURCE
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY ps.SOURCE#

DROP VIEW IF EXISTS bi_players_by_date_and_source_and_game#
CREATE VIEW bi_players_by_date_and_source_and_game AS
	SELECT DATE(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pg.GAME_TYPE AS GAME
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY DATE(pcs.END_TS),ps.SOURCE,pg.GAME_TYPE#

DROP VIEW IF EXISTS bi_players_by_week_and_source_and_game#
CREATE VIEW bi_players_by_week_and_source_and_game AS
	SELECT last_day_of_week(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pg.GAME_TYPE AS GAME
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEARWEEK(pcs.END_TS,5),ps.SOURCE,pg.GAME_TYPE#

DROP VIEW IF EXISTS bi_players_by_month_and_source_and_game#
CREATE VIEW bi_players_by_month_and_source_and_game AS
	SELECT last_day(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pg.GAME_TYPE AS GAME
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEAR(pcs.END_TS),MONTH(pcs.END_TS),ps.SOURCE,pg.GAME_TYPE#

DROP VIEW IF EXISTS bi_players_by_source_and_game#
CREATE VIEW bi_players_by_source_and_game AS
	SELECT COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, ps.SOURCE AS SOURCE,
		pg.GAME_TYPE AS GAME
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY ps.SOURCE,pg.GAME_TYPE#
	
DROP VIEW IF EXISTS bi_players_by_date_and_source_and_platform#
CREATE VIEW bi_players_by_date_and_source_and_platform AS
	SELECT DATE(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.SESSION_PLATFORM AS SESSION_PLATFORM
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY DATE(pcs.END_TS),ps.SOURCE,pcs.SESSION_PLATFORM#

DROP VIEW IF EXISTS bi_players_by_week_and_source_and_platform#
CREATE VIEW bi_players_by_week_and_source_and_platform AS
	SELECT last_day_of_week(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.SESSION_PLATFORM AS SESSION_PLATFORM
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEARWEEK(pcs.END_TS,5),ps.SOURCE,pcs.SESSION_PLATFORM#


DROP VIEW IF EXISTS bi_players_by_month_and_source_and_platform#
CREATE VIEW bi_players_by_month_and_source_and_platform AS
	SELECT last_day(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.SESSION_PLATFORM AS SESSION_PLATFORM
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEAR(pcs.END_TS),MONTH(pcs.END_TS),ps.SOURCE,pcs.SESSION_PLATFORM#

DROP VIEW IF EXISTS bi_players_by_source_and_platform#
CREATE VIEW bi_players_by_source_and_platform AS
	SELECT COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.SESSION_PLATFORM AS SESSION_PLATFORM
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY ps.SOURCE,pcs.SESSION_PLATFORM#
	
DROP VIEW IF EXISTS bi_players_by_date_and_source_and_lobby#
CREATE VIEW bi_players_by_date_and_source_and_lobby AS
	SELECT DATE(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.LOGIN_GAME_LOBBY AS LOGIN_GAME_LOBBY
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY DATE(pcs.END_TS),ps.SOURCE,pcs.LOGIN_GAME_LOBBY#

DROP VIEW IF EXISTS bi_players_by_week_and_source_and_lobby#
CREATE VIEW bi_players_by_week_and_source_and_lobby AS
	SELECT last_day_of_week(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.LOGIN_GAME_LOBBY AS LOGIN_GAME_LOBBY
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEARWEEK(pcs.END_TS,5),ps.SOURCE,pcs.LOGIN_GAME_LOBBY#


DROP VIEW IF EXISTS bi_players_by_month_and_source_and_lobby#
CREATE VIEW bi_players_by_month_and_source_and_lobby AS
	SELECT last_day(pcs.END_TS) AS END_DATE,COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.LOGIN_GAME_LOBBY AS LOGIN_GAME_LOBBY
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY YEAR(pcs.END_TS),MONTH(pcs.END_TS),ps.SOURCE,pcs.LOGIN_GAME_LOBBY#

DROP VIEW IF EXISTS bi_players_by_source_and_lobby#
CREATE VIEW bi_players_by_source_and_lobby AS
	SELECT COUNT(DISTINCT pcs.ACCOUNT_ID) AS USERS, COUNT(pg.ACCOUNT_ID) AS PLAYERS, 
		ps.SOURCE AS SOURCE,pcs.LOGIN_GAME_LOBBY AS LOGIN_GAME_LOBBY
	FROM PLAYER_CLOSED_SESSION pcs
	JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
	JOIN LOBBY_USER lu USING(PLAYER_ID)
	JOIN rpt_player_sources_mv ps USING (USER_ID)
	LEFT JOIN PLAYER_GAME_SESSION pg USING(SESSION_ID)
	GROUP BY ps.SOURCE,pcs.LOGIN_GAME_LOBBY#
	