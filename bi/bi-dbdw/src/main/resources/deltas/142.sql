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

DROP TABLE IF EXISTS PLAYER_GAME_SESSION#
