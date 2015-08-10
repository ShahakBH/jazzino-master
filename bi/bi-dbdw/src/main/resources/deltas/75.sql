ALTER TABLE rpt_users_daily 
CHANGE COLUMN platform platform VARCHAR(64) NOT NULL DEFAULT '', 
DROP PRIMARY KEY, 
ADD PRIMARY KEY (reportDate, platform)#

ALTER TABLE rpt_users_weekly 
CHANGE COLUMN platform platform VARCHAR(64) NOT NULL DEFAULT '', 
DROP PRIMARY KEY, 
ADD PRIMARY KEY (weekEnding, platform)#

ALTER TABLE rpt_users_monthly 
CHANGE COLUMN platform platform VARCHAR(64) NOT NULL DEFAULT '', 
DROP PRIMARY KEY, 
ADD PRIMARY KEY (monthEnding, platform)#

DROP PROCEDURE IF EXISTS extractAccountActivityTimed#
DROP PROCEDURE IF EXISTS extractAccountActivity#

DROP TRIGGER IF EXISTS account_activity_trigger_del#

CREATE TABLE IF NOT EXISTS rpt_account_activity_weekly (
  PLAYER_ID bigint(20) NOT NULL,
  AUDIT_DATE date NOT NULL,
  GAME_TYPE varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (PLAYER_ID,AUDIT_DATE,GAME_TYPE),
  KEY IDX_AUDIT_DATE (AUDIT_DATE)
) #

CREATE TABLE IF NOT EXISTS rpt_account_activity_monthly (
  PLAYER_ID bigint(20) NOT NULL,
  AUDIT_DATE date NOT NULL,
  GAME_TYPE varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (PLAYER_ID,AUDIT_DATE,GAME_TYPE),
  KEY IDX_AUDIT_DATE (AUDIT_DATE)
) #

CREATE TABLE IF NOT EXISTS rpt_activity_by_account_id_weekly (
  ACCOUNT_ID bigint(20) NOT NULL,
  AUDIT_DATE date NOT NULL,
  GAME_TYPE varchar(255) DEFAULT '',
  PLATFORM varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (ACCOUNT_ID,AUDIT_DATE,PLATFORM),
  KEY IDX_AUDIT_DATE (AUDIT_DATE)
) #

CREATE TABLE IF NOT EXISTS rpt_activity_by_account_id_monthly (
  ACCOUNT_ID bigint(20) NOT NULL,
  AUDIT_DATE date NOT NULL,
  GAME_TYPE varchar(255) DEFAULT '',
  PLATFORM varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (ACCOUNT_ID,AUDIT_DATE,PLATFORM),
  KEY IDX_AUDIT_DATE (AUDIT_DATE)
) #


DROP FUNCTION IF EXISTS last_day_of_week#
CREATE FUNCTION last_day_of_week (dat DATE)
RETURNS DATE DETERMINISTIC
RETURN DATE(DATE_ADD(dat, INTERVAL(6 - WEEKDAY(dat)) DAY));
#

CREATE PROCEDURE extractAccountActivity()
begin
	declare lastAuditCommandAutoId int;
	declare maxAuditCommandAutoId int;
	
    -- update player activity
    select val into @lastAuditCommandAutoId from rpt_report_status where report_action = 'distinctPlayers';
    select max(auto_id) into @maxAuditCommandAutoId from AUDIT_COMMAND;

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

DROP TRIGGER IF EXISTS account_activity_trigger#
CREATE TRIGGER account_activity_trigger
AFTER INSERT ON rpt_account_activity
FOR EACH ROW
BEGIN
  INSERT INTO rpt_players_daily(reportDate,players,game_type)
  	VALUES(DATE(NEW.AUDIT_DATE),1,NEW.GAME_TYPE)
  	ON DUPLICATE KEY
  	UPDATE players = players+1;
END
#

DROP TRIGGER IF EXISTS account_activity_weekly_trigger#
CREATE TRIGGER account_activity_weekly_trigger
AFTER INSERT ON rpt_account_activity_weekly
FOR EACH ROW
BEGIN
  INSERT INTO rpt_players_weekly(weekEnding,players,game_type)
  	VALUES(DATE(NEW.AUDIT_DATE),1,NEW.GAME_TYPE)
  	ON DUPLICATE KEY
  	UPDATE players = players+1;
END
#

DROP TRIGGER IF EXISTS account_activity_monthly_trigger#
CREATE TRIGGER account_activity_monthly_trigger
AFTER INSERT ON rpt_account_activity_monthly
FOR EACH ROW
BEGIN
  INSERT INTO rpt_players_monthly(monthEnding,players,game_type)
  	VALUES(DATE(NEW.AUDIT_DATE),1,NEW.GAME_TYPE)
  	ON DUPLICATE KEY
  	UPDATE players = players+1;
END
#

DROP TRIGGER IF EXISTS account_actions#

DROP PROCEDURE IF EXISTS account_inserts#
CREATE PROCEDURE account_inserts(IN account_id_val INT(11), IN tsstarted_val DATETIME, IN platform_val VARCHAR(64))
BEGIN
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
END;
#

CREATE TRIGGER account_actions
AFTER INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  CALL account_inserts(NEW.ACCOUNT_ID,NEW.TSSTARTED,NEW.PLATFORM);
END
#

DROP TRIGGER IF EXISTS account_user_activity_trigger#
CREATE TRIGGER account_user_activity_trigger
AFTER INSERT ON rpt_activity_by_account_id
FOR EACH ROW
BEGIN
  IF NEW.PLATFORM = '' THEN
  INSERT INTO rpt_users_daily(reportDate,users,platform)
  	VALUES(DATE(NEW.AUDIT_DATE),1,'')
  	ON DUPLICATE KEY
  	UPDATE users = users+1;
  END IF;
END
#

DROP TRIGGER IF EXISTS account_user_activity_weekly_trigger#
DROP TRIGGER IF EXISTS account_user_activity_monthly_trigger#

DROP TRIGGER IF EXISTS account_user_activity_trigger_weekly#
CREATE TRIGGER account_user_activity_trigger_weekly
AFTER INSERT ON rpt_activity_by_account_id_weekly
FOR EACH ROW
BEGIN
  IF NEW.PLATFORM = '' THEN
  INSERT INTO rpt_users_weekly(weekEnding,users,platform)
  	VALUES(DATE(NEW.AUDIT_DATE),1,'')
  	ON DUPLICATE KEY
  	UPDATE users = users+1;
  END IF;
END
#

DROP TRIGGER IF EXISTS account_user_activity_trigger_monthly#
CREATE TRIGGER account_user_activity_trigger_monthly
AFTER INSERT ON rpt_activity_by_account_id_monthly
FOR EACH ROW
BEGIN
  IF NEW.PLATFORM = '' THEN
  INSERT INTO rpt_users_monthly(monthEnding,users,platform)
  	VALUES(DATE(NEW.AUDIT_DATE),1,'')
  	ON DUPLICATE KEY
  	UPDATE users = users+1;
  END IF;
END
#

CALL extractAccountActivity()#

DROP EVENT IF EXISTS extractAccountActivity#
CREATE EVENT evtExtractAccountActivity 
ON SCHEDULE EVERY 1 MINUTE 
COMMENT 'Fill the rpt_player_sources_mv materialized view'
DO CALL extractAccountActivity()#
