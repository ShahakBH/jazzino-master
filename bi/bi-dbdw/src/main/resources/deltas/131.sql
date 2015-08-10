CREATE TABLE IF NOT EXISTS `rpt_players_by_day` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  GAME_TYPE varchar(32) NOT NULL,
  PRIMARY KEY (ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_day` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  PRIMARY KEY (ACCOUNT_ID, AUDIT_DATE)
)#

CREATE TABLE IF NOT EXISTS rpt_account_sources_mv (
  ACCOUNT_ID bigint(21) NOT NULL,
  SOURCE varchar(255) DEFAULT NULL,
  TSCREATED timestamp NULL DEFAULT NULL,
  PRIMARY KEY (ACCOUNT_ID),
  KEY `IDX_TSCREATED` (TSCREATED),
  KEY `IDX_SOURCE` (SOURCE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_day_source_count` (
  AUDIT_DATE date DEFAULT NULL,
  USERS int(11) unsigned DEFAULT 0,
  PLAYERS int(11) unsigned DEFAULT 0,
  SOURCE varchar(255) DEFAULT NULL,
  PRIMARY KEY (AUDIT_DATE,SOURCE)
)#

CREATE TABLE IF NOT EXISTS `rpt_players_by_week` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  GAME_TYPE varchar(32) NOT NULL,
  PRIMARY KEY (ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_week` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  PRIMARY KEY (ACCOUNT_ID, AUDIT_DATE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_week_source_count` (
  AUDIT_DATE date DEFAULT NULL,
  USERS int(11) unsigned DEFAULT 0,
  PLAYERS int(11) unsigned DEFAULT 0,
  SOURCE varchar(255) DEFAULT NULL,
  PRIMARY KEY (AUDIT_DATE,SOURCE)
)#

CREATE TABLE IF NOT EXISTS `rpt_players_by_month` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  GAME_TYPE varchar(32) NOT NULL,
  PRIMARY KEY (ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_month` (
  ACCOUNT_ID bigint(21) NOT NULL,
  AUDIT_DATE date DEFAULT NULL,
  PRIMARY KEY (ACCOUNT_ID, AUDIT_DATE)
)#

CREATE TABLE IF NOT EXISTS `rpt_users_by_month_source_count` (
  AUDIT_DATE date DEFAULT NULL,
  USERS int(11) unsigned DEFAULT 0,
  PLAYERS int(11) unsigned DEFAULT 0,
  SOURCE varchar(255) DEFAULT NULL,
  PRIMARY KEY (AUDIT_DATE,SOURCE)
)#

DROP TABLE IF EXISTS rpt_player_sources_mv#
DROP VIEW IF EXISTS rpt_player_sources_mv#
CREATE VIEW rpt_player_sources_mv AS
SELECT lu.USER_ID AS USER_ID,r.SOURCE,r.TSCREATED
FROM rpt_account_sources_mv r
JOIN PLAYER_DEFINITION p USING (ACCOUNT_ID)
JOIN LOBBY_USER lu USING (PLAYER_ID)#

DROP PROCEDURE IF EXISTS extractSessionState#
CREATE PROCEDURE extractSessionState(IN actual_time DATETIME)
BEGIN
		INSERT IGNORE INTO rpt_players_by_day(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_DATE, GAME_TYPE FROM rpt_tmp_latest_commands);
		-- The following line is absolutely needed for the "counting triggers" to consistently count players
		INSERT IGNORE INTO rpt_players_by_day(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_DATE, '' FROM rpt_tmp_latest_commands);
		INSERT IGNORE INTO rpt_users_by_day(ACCOUNT_ID,AUDIT_DATE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_DATE FROM rpt_tmp_latest_commands);
			
		INSERT IGNORE INTO rpt_players_by_week(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_WEEK, GAME_TYPE FROM rpt_tmp_latest_commands);
		INSERT IGNORE INTO rpt_players_by_week(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_WEEK, '' FROM rpt_tmp_latest_commands);
		INSERT IGNORE INTO rpt_users_by_week(ACCOUNT_ID,AUDIT_DATE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_WEEK FROM rpt_tmp_latest_commands);
			
		INSERT IGNORE INTO rpt_players_by_month(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_MONTH, GAME_TYPE FROM rpt_tmp_latest_commands);
		INSERT IGNORE INTO rpt_players_by_month(ACCOUNT_ID,AUDIT_DATE,GAME_TYPE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_MONTH, '' FROM rpt_tmp_latest_commands);
		INSERT IGNORE INTO rpt_users_by_month(ACCOUNT_ID,AUDIT_DATE)
			(SELECT DISTINCT ACCOUNT_ID, AUDIT_MONTH FROM rpt_tmp_latest_commands);
		
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

DROP VIEW IF EXISTS bi_players_by_source#
DROP VIEW IF EXISTS bi_players_by_week_and_source#
DROP VIEW IF EXISTS bi_players_by_month_and_source#
DROP VIEW IF EXISTS bi_players_by_source#
DROP VIEW IF EXISTS bi_players_by_date_and_source_and_game#
DROP VIEW IF EXISTS bi_players_by_week_and_source_and_game#
DROP VIEW IF EXISTS bi_players_by_month_and_source_and_game#
DROP VIEW IF EXISTS bi_players_by_source_and_game#
DROP VIEW IF EXISTS bi_players_by_date_and_source_and_platform#
DROP VIEW IF EXISTS bi_players_by_week_and_source_and_platform#
DROP VIEW IF EXISTS bi_players_by_month_and_source_and_platform#
DROP VIEW IF EXISTS bi_players_by_source_and_platform#
DROP VIEW IF EXISTS bi_players_by_date_and_source_and_lobby#
DROP VIEW IF EXISTS bi_players_by_week_and_source_and_lobby#
DROP VIEW IF EXISTS bi_players_by_month_and_source_and_lobby#
DROP VIEW IF EXISTS bi_players_by_source_and_lobby#
DROP VIEW IF EXISTS bi_players_by_country_and_date#
DROP VIEW IF EXISTS bi_players_by_country_and_week#
DROP VIEW IF EXISTS bi_players_by_country_and_month#
DROP VIEW IF EXISTS bi_players_by_country_and_date_and_game#
DROP VIEW IF EXISTS bi_players_by_country_and_week_and_game#
DROP VIEW IF EXISTS bi_players_by_country_and_month_and_game#

DROP VIEW IF EXISTS bi_rt2m_players_by_game#
CREATE VIEW bi_rt2m_players_by_game AS
	SELECT psc.ACTUAL_GAME,COUNT(psc.ACCOUNT_ID) AS USERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 2 MINUTE
	GROUP BY psc.ACTUAL_GAME
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt30m_players_by_game#
CREATE VIEW bi_rt30m_players_by_game AS
	SELECT psc.ACTUAL_GAME,COUNT(psc.ACCOUNT_ID) AS USERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 30 MINUTE
	GROUP BY psc.ACTUAL_GAME
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt2m_players_by_country#
CREATE VIEW bi_rt2m_players_by_country AS
	SELECT gll.COUNTRY,COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.GEOLOC_ACCOUNTS gla USING (ACCOUNT_ID)
	LEFT JOIN strataproddw.GEOLOC_LOCATIONS gll USING (LOCATION_ID)
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 2 MINUTE
	GROUP BY gll.COUNTRY
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt30m_players_by_country#
CREATE VIEW bi_rt30m_players_by_country AS
	SELECT gll.COUNTRY,COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.GEOLOC_ACCOUNTS gla USING (ACCOUNT_ID)
	LEFT JOIN strataproddw.GEOLOC_LOCATIONS gll USING (LOCATION_ID)
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 30 MINUTE
	GROUP BY gll.COUNTRY
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt2m_players_by_country_and_region#
CREATE VIEW bi_rt2m_players_by_country_and_region AS
	SELECT gll.COUNTRY,gll.REGION,
		COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.GEOLOC_ACCOUNTS gla USING (ACCOUNT_ID)
	LEFT JOIN strataproddw.GEOLOC_LOCATIONS gll USING (LOCATION_ID)
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 2 MINUTE
	GROUP BY gll.COUNTRY,gll.REGION
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt30m_players_by_country_and_region#
CREATE VIEW bi_rt30m_players_by_country_and_region AS
	SELECT gll.COUNTRY,gll.REGION,
		COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.GEOLOC_ACCOUNTS gla USING (ACCOUNT_ID)
	LEFT JOIN strataproddw.GEOLOC_LOCATIONS gll USING (LOCATION_ID)
	WHERE psc.LAST_ACTIVITY_TS >= now() - INTERVAL 30 MINUTE
	GROUP BY gll.COUNTRY,gll.REGION
	ORDER BY COUNT(ACCOUNT_ID) DESC
#

DROP VIEW IF EXISTS bi_rt2m_players_by_source#
CREATE VIEW bi_rt2m_players_by_source AS
	SELECT s.SOURCE AS SOURCE, COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.rpt_account_sources_mv s USING (ACCOUNT_ID)
	WHERE LAST_ACTIVITY_TS >= now() - INTERVAL 2 MINUTE
	GROUP BY SOURCE
	ORDER BY COUNT(ACCOUNT_ID) DESC#

DROP VIEW IF EXISTS bi_rt30m_players_by_source#
CREATE VIEW bi_rt30m_players_by_source AS
	SELECT s.SOURCE AS SOURCE, COUNT(psc.ACCOUNT_ID) AS USERS, SUM(IF(psc.ACTUAL_GAME = 'NONE',0,1)) AS PLAYERS
	FROM strataproddw.PLAYER_STATUS_CURRENT psc
	LEFT JOIN strataproddw.rpt_account_sources_mv s USING (ACCOUNT_ID)
	WHERE LAST_ACTIVITY_TS >= now() - INTERVAL 30 MINUTE
	GROUP BY SOURCE
	ORDER BY COUNT(ACCOUNT_ID) DESC#

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
        
    INSERT IGNORE INTO rpt_users_by_day(ACCOUNT_ID,AUDIT_DATE)
    	VALUES(account_id_val,DATE(tsstarted_val));
    	
    INSERT IGNORE INTO rpt_users_by_week(ACCOUNT_ID,AUDIT_DATE)
    	VALUES(account_id_val,last_day_of_week(tsstarted_val));
    	
    INSERT IGNORE INTO rpt_users_by_month(ACCOUNT_ID,AUDIT_DATE)
    	VALUES(account_id_val,last_day(tsstarted_val));

    INSERT IGNORE INTO rpt_activity_by_account_id(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,DATE(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,DATE(tsstarted_val),'');

    INSERT IGNORE INTO rpt_activity_by_account_id_weekly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day_of_week(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day_of_week(tsstarted_val),'');

    INSERT IGNORE INTO rpt_activity_by_account_id_monthly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day(tsstarted_val),'');
END
#

-- Updating the sources tables with reliable information

DROP TRIGGER IF EXISTS ad_tracking_trigger#
CREATE TRIGGER ad_tracking_trigger
AFTER INSERT ON AD_TRACKING
FOR EACH ROW
BEGIN
	REPLACE INTO rpt_account_sources_mv(ACCOUNT_ID,SOURCE,TSCREATED)
		SELECT p.ACCOUNT_ID,NEW.AD_CODE,NEW.REGISTRATION_TS
		FROM LOBBY_USER lu
		JOIN PLAYER_DEFINITION p USING(PLAYER_ID)
		WHERE lu.USER_ID = NEW.USER_ID;
END
#

DROP TRIGGER IF EXISTS lobby_user_trigger#
CREATE TRIGGER lobby_user_trigger
AFTER INSERT ON LOBBY_USER
FOR EACH ROW
BEGIN
	INSERT IGNORE INTO rpt_account_sources_mv(ACCOUNT_ID,SOURCE,TSCREATED)
		SELECT p.ACCOUNT_ID,
		CASE WHEN a.AD_CODE IS NULL THEN (CASE WHEN NEW.REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE a.AD_CODE END AS SOURCE,
		CASE WHEN a.REGISTRATION_TS IS NULL THEN NEW.TSREG ELSE a.REGISTRATION_TS END AS TSCREATED
		FROM strataproddw.PLAYER_DEFINITION p
		LEFT JOIN AD_TRACKING a ON a.USER_ID=NEW.USER_ID
		WHERE p.PLAYER_ID = NEW.PLAYER_ID;
END
#

DROP TRIGGER IF EXISTS player_definition_trigger#
CREATE TRIGGER player_definition_trigger
AFTER INSERT ON PLAYER_DEFINITION
FOR EACH ROW
BEGIN
	INSERT IGNORE INTO rpt_account_sources_mv(ACCOUNT_ID,SOURCE,TSCREATED)
		SELECT NEW.ACCOUNT_ID,
		CASE WHEN a.AD_CODE IS NULL THEN (CASE WHEN l.REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE a.AD_CODE END AS SOURCE,
		CASE WHEN a.REGISTRATION_TS IS NULL THEN l.TSREG ELSE a.REGISTRATION_TS END AS TSCREATED
		FROM strataproddw.LOBBY_USER l
		LEFT JOIN AD_TRACKING a USING(USER_ID)
		WHERE l.PLAYER_ID = NEW.PLAYER_ID;
END
#

DROP PROCEDURE IF EXISTS `strataproddw`.`rptFillSourcesTable`#
DROP EVENT IF EXISTS evtFillSourceTables#

-- Automatically filling periodic statistics

DROP TRIGGER IF EXISTS users_by_day_trigger#
CREATE TRIGGER users_by_day_trigger
AFTER INSERT ON rpt_users_by_day
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
  
  SELECT SOURCE into source_val
  FROM rpt_account_sources_mv
  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
	
  INSERT INTO rpt_users_by_day_source_count(AUDIT_DATE,USERS,SOURCE)
  	VALUES(NEW.AUDIT_DATE,1,source_val)
  	ON DUPLICATE KEY
  	UPDATE USERS = USERS+1;
END
#

DROP TRIGGER IF EXISTS players_by_day_trigger#
CREATE TRIGGER players_by_day_trigger
AFTER INSERT ON rpt_players_by_day
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
 
  IF NEW.GAME_TYPE = '' THEN
	  SELECT SOURCE into source_val
	  FROM rpt_account_sources_mv
	  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
		
	  INSERT INTO rpt_users_by_day_source_count(AUDIT_DATE,PLAYERS,SOURCE)
	  	VALUES(NEW.AUDIT_DATE,1,source_val)
	  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
  END IF;
END
#

DROP TRIGGER IF EXISTS users_by_week_trigger#
CREATE TRIGGER users_by_week_trigger
AFTER INSERT ON rpt_users_by_week
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';
  
  SELECT SOURCE into source_val
  FROM rpt_account_sources_mv
  WHERE ACCOUNT_ID = NEW.ACCOUNT_ID;
	
  INSERT INTO rpt_users_by_week_source_count(AUDIT_DATE,USERS,SOURCE)
  	VALUES(NEW.AUDIT_DATE,1,source_val)
  	ON DUPLICATE KEY
  	UPDATE USERS = USERS+1;
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
	  	VALUES(NEW.AUDIT_DATE,1,source_val)
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
  	VALUES(NEW.AUDIT_DATE,1,source_val)
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
	  	VALUES(NEW.AUDIT_DATE,1,source_val)
	  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
  END IF;
END
#
