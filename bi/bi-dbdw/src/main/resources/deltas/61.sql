CREATE TABLE PLAYER_INFO (
  PLAYER_ID BIGINT(20) NOT NULL ,
  REGISTRATION_PLATFORM VARCHAR(32) NULL ,
  REGISTRATION_GAME VARCHAR(64) NULL ,
  FIRST_PURCHASE DATETIME NULL ,
  PRIMARY KEY (PLAYER_ID) ,
  INDEX PLATFORM_KEY (REGISTRATION_PLATFORM ASC) )#
  
DROP PROCEDURE IF EXISTS external_transactions_inserts#
CREATE PROCEDURE external_transactions_inserts(IN player_id_val BIGINT(20), IN first_purchase_val DATETIME)
BEGIN
  INSERT INTO PLAYER_INFO(PLAYER_ID, FIRST_PURCHASE)
    VALUES (player_id_val,first_purchase_val)
      ON DUPLICATE KEY
      UPDATE FIRST_PURCHASE = 
          IF(FIRST_PURCHASE IS NULL,VALUES(FIRST_PURCHASE),FIRST_PURCHASE);
END
#

DROP PROCEDURE IF EXISTS scanExternalTransactions#
CREATE PROCEDURE scanExternalTransactions(IN max_session BIGINT(20))
BEGIN
  DECLARE ok INT DEFAULT 0;
  DECLARE player_id_val BIGINT(20);
  DECLARE tsstarted_val DATETIME;
  DECLARE session_cursor CURSOR FOR
    SELECT 
      p.PLAYER_ID,x.MESSAGE_TIMESTAMP
    FROM EXTERNAL_TRANSACTION x
    JOIN strataprod.PLAYER p ON x.ACCOUNT_ID=p.ACCOUNT_ID
    WHERE x.AUTO_ID > max_session AND x.EXTERNAL_TRANSACTION_STATUS = 'SUCCESS'
    ORDER BY x.MESSAGE_TIMESTAMP;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET ok = 1;
  OPEN session_cursor;
  session_loop: LOOP
    FETCH session_cursor INTO player_id_val, tsstarted_val;
    IF ok THEN LEAVE session_loop; END IF;

    CALL external_transactions_inserts(player_id_val,tsstarted_val);
  END LOOP session_loop;  
END
#

CALL scanExternalTransactions(1)#

DROP PROCEDURE scanExternalTransactions#

DROP TRIGGER IF EXISTS external_transactions_actions#
CREATE TRIGGER external_transactions_actions
AFTER INSERT ON EXTERNAL_TRANSACTION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  SELECT PLAYER_ID FROM strataprod.PLAYER WHERE ACCOUNT_ID = NEW.ACCOUNT_ID INTO player_id_val;

  CALL external_transactions_inserts(player_id_val,NEW.MESSAGE_TIMESTAMP);
END#


DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE account_inserts(IN player_id_val BIGINT(20), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64))
BEGIN
  INSERT INTO PLAYER_INFO(PLAYER_ID, REGISTRATION_PLATFORM)
      VALUES (player_id_val,platform_val)
      ON DUPLICATE KEY
      UPDATE REGISTRATION_PLATFORM = 
        IF(REGISTRATION_PLATFORM IS NULL,VALUES(REGISTRATION_PLATFORM),REGISTRATION_PLATFORM);
        
    INSERT IGNORE INTO rpt_account_activity(player_id, audit_date, game_type)
      VALUES(player_id_val,DATE(tsstarted_val),'');
END
#

DROP TRIGGER IF EXISTS account_actions#
CREATE TRIGGER account_actions
AFTER INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  SELECT PLAYER_ID FROM strataprod.PLAYER WHERE ACCOUNT_ID = NEW.ACCOUNT_ID INTO player_id_val;

  CALL account_inserts(player_id_val,NEW.TSSTARTED,NEW.PLATFORM);
END
#

DROP PROCEDURE IF EXISTS scanAccountSessions#
CREATE PROCEDURE scanAccountSessions(IN max_session BIGINT(20))
BEGIN
  DECLARE ok INT DEFAULT 0;
  DECLARE player_id_val BIGINT(20);
  DECLARE tsstarted_val DATETIME;
  DECLARE platform_val VARCHAR(64); 
  DECLARE session_cursor CURSOR FOR
    SELECT 
      p.PLAYER_ID,a.TSSTARTED,a.PLATFORM
    FROM ACCOUNT_SESSION a
    JOIN strataprod.PLAYER p ON a.ACCOUNT_ID=p.ACCOUNT_ID
    WHERE a.SESSION_ID > max_session
    ORDER BY a.TSSTARTED;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET ok = 1;
  OPEN session_cursor;
  session_loop: LOOP
    FETCH session_cursor INTO player_id_val, tsstarted_val, platform_val;
    IF ok THEN LEAVE session_loop; END IF;

    CALL account_inserts(player_id_val,tsstarted_val,platform_val);
  END LOOP session_loop;  
END
#

DROP PROCEDURE IF EXISTS refreshAccountSessions#
CREATE PROCEDURE refreshAccountSessions()
BEGIN
  declare lastAccountSessionId int;
  
  select val into @lastAccountSessionId from rpt_report_status where report_action = 'distinctUsers';

  CALL scanAccountSessions(@lastAccountSessionId);
END
#

call refreshAccountSessions()#

DROP PROCEDURE refreshAccountSessions#

DROP PROCEDURE scanAccountSessions#

DROP PROCEDURE IF EXISTS extractAccountActivity#
#