DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE `account_inserts`(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64), IN start_page_val VARCHAR(2048), IN referer_val VARCHAR(2048))
BEGIN
    DECLARE user_id_val BIGINT(20) DEFAULT 0;
    DECLARE player_id_val BIGINT(20) DEFAULT 0;
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
    
   SELECT PLAYER_ID INTO player_id_val
   FROM PLAYER_DEFINITION WHERE ACCOUNT_ID = account_id_val;
   
   SELECT USER_ID INTO user_id_val
   FROM LOBBY_USER WHERE PLAYER_ID = player_id_val;
     
   UPDATE rpt_recent_registrations SET PLATFORM = platform_val, START_PAGE = start_page_val WHERE USER_ID = user_id_val AND PLATFORM = '';

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
     	
     IF referer_val<>'' AND referer_val IS NOT NULL THEN
	     INSERT IGNORE INTO bi_session_sources(PLAYER_ID,SOURCE,FIRST_LOGIN)
    	 	VALUES(player_id_val,referer_val,DATE(tsstarted_val));
     END IF;
END#
