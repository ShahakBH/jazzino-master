DROP TRIGGER IF EXISTS account_actions#

DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE account_inserts(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64), IN start_page_val VARCHAR(2048))
BEGIN
	DECLARE user_id_val BIGINT(20) DEFAULT 0;
	declare maxUserId BIGINT(20) DEFAULT 0;
	
	SELECT MAX(USER_ID) INTO @maxUserId FROM rpt_recent_registrations;
	
	IF NOT @maxUserId IS NULL THEN
		INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
    	SELECT USER_ID,TSREG,PROVIDER_NAME,IF(PROVIDER_NAME='YAZINO',USER_ID,EXTERNAL_ID),account_id_val,FIRST_NAME
    		FROM strataprod.LOBBY_USER
    		WHERE USER_ID > @maxUserId;
    ELSE
    	INSERT IGNORE INTO rpt_recent_registrations(USER_ID,AUDIT_TIME,RPX,EXTERNAL_ID,ACCOUNT_ID,FIRST_NAME)
    	SELECT USER_ID,TSREG,PROVIDER_NAME,EXTERNAL_ID,account_id_val,FIRST_NAME
    		FROM strataprod.LOBBY_USER
    		WHERE TSREG > now() - INTERVAL 90 MINUTE;
    END IF;

	SELECT USER_ID INTO @user_id_val FROM strataprod.PLAYER WHERE ACCOUNT_ID = account_id_val ORDER BY USER_ID DESC LIMIT 1;

	UPDATE rpt_recent_registrations SET PLATFORM = platform_val, START_PAGE = start_page_val WHERE USER_ID = @user_id_val AND PLATFORM = '';

  	INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, REGISTRATION_PLATFORM)
      VALUES (account_id_val,platform_val)
      ON DUPLICATE KEY
      UPDATE REGISTRATION_PLATFORM = 
        IF(REGISTRATION_PLATFORM IS NULL,VALUES(REGISTRATION_PLATFORM),REGISTRATION_PLATFORM);
        
    INSERT IGNORE INTO rpt_activity_by_account_id(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,DATE(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,DATE(tsstarted_val),'');
      
    INSERT IGNORE INTO rpt_activity_by_account_id_weekly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day_of_week(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day_of_week(tsstarted_val),'');

    INSERT IGNORE INTO rpt_activity_by_account_id_monthly(ACCOUNT_ID, AUDIT_DATE, PLATFORM)
      VALUES(account_id_val,last_day(tsstarted_val),IF(platform_val='','UNKNOWN',platform_val)),
      (account_id_val,last_day(tsstarted_val),'');
END#

CREATE TRIGGER account_actions
AFTER INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  CALL account_inserts(NEW.ACCOUNT_ID,NEW.TSSTARTED,NEW.PLATFORM,NEW.START_PAGE);
END
#
