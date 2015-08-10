CREATE TABLE IF NOT EXISTS rpt_player_sources_mv (
  USER_ID BIGINT(20) NOT NULL ,
  SOURCE VARCHAR(255) NULL ,
  TSCREATED TIMESTAMP NULL ,
  PRIMARY KEY (USER_ID) ,
  INDEX IDX_TSCREATED (TSCREATED ASC) ,
  INDEX IDX_SOURCE (SOURCE ASC) )#
  
DROP PROCEDURE IF EXISTS rptFillSourcesTable#
CREATE PROCEDURE rptFillSourcesTable()
BEGIN 
	DECLARE lastSourcesId BIGINT(20) DEFAULT NULL;

	SELECT MAX(USER_ID) FROM rpt_player_sources_mv INTO	@lastSourcesId;
	
	if get_lock('strataproddw.fill_sources_tables_lock', 0) = 1 then
	  INSERT IGNORE INTO rpt_player_sources_mv(USER_ID,SOURCE,TSCREATED)
		SELECT l.USER_ID,
		CASE WHEN a.AD_CODE IS NULL THEN (CASE WHEN l.REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE a.AD_CODE END AS SOURCE,
		CASE WHEN a.REGISTRATION_TS IS NULL THEN p.tscreated ELSE a.REGISTRATION_TS END AS TSCREATED
		FROM strataprod.PLAYER p
		JOIN strataprod.LOBBY_USER l ON l.USER_ID=p.USER_ID
		LEFT JOIN AD_TRACKING a ON a.USER_ID=l.USER_ID
		WHERE l.USER_ID >= @lastSourcesId - 25;
		
		do release_lock('strataproddw.fill_sources_tables_lock');
	end if;
END#

DROP EVENT IF EXISTS evtFillSourceTables#
CREATE EVENT evtFillSourceTables 
ON SCHEDULE EVERY 5 MINUTE 
COMMENT 'Fill the rpt_player_sources_mv materialized view'
DO CALL rptFillSourcesTable()#
