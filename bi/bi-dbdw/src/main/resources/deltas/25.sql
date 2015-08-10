--                      WARNING!
-- You must run /src/main/sh/grant-file-privileges.sh before runnung this

DROP VIEW IF EXISTS mm_external_transactions#
-- CREATE VIEW mm_external_transactions AS
-- select
-- 	ET.AUTO_ID AS AUTO_ID,lu.PROVIDER_NAME AS PARTNER_ID,lu.EXTERNAL_ID AS EXTERNAL_ID,P.USER_ID AS USER_ID,ET.GAME_TYPE AS GAME_TYPE,
-- 	ET.AMOUNT_CHIPS AS AMOUNT_CHIPS,ET.AMOUNT AS AMOUNT,ET.CURRENCY_CODE AS CURRENCY_CODE,ET.TRANSACTION_TYPE AS TRANSACTION_TYPE,
-- 	ET.MESSAGE_TIMESTAMP AS TIMESTAMP
-- from EXTERNAL_TRANSACTION ET
-- join strataprod.PLAYER P
-- left join strataprod.LOBBY_USER lu ON P.USER_ID=lu.USER_ID
-- where (P.ACCOUNT_ID = ET.ACCOUNT_ID)
-- #

DROP TABLE IF EXISTS rpt_mm_export_status#
CREATE TABLE rpt_mm_export_status (
  ID VARCHAR(16) NOT NULL,
  LAST_ID BIGINT NULL,
PRIMARY KEY (`ID`) )#

DROP PROCEDURE IF EXISTS mm_export_latest_purchases#
CREATE PROCEDURE mm_export_latest_purchases(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,GAME_TYPE,AMOUNT_CHIPS,AMOUNT,CURRENCY_CODE,TRANSACTION_TYPE,TIMESTAMP,AUTO_ID ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @lastId = NULL;
  SELECT LAST_ID FROM rpt_mm_export_status WHERE ID='PURCHASES' INTO @lastId;

  IF (@lastId IS NULL) THEN
    SET @request_clauses = ' FROM mm_external_transactions';
  ELSE
    SET @request_clauses = CONCAT(' FROM mm_external_transactions WHERE AUTO_ID > ', @lastId);
  END IF;

  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#

DROP VIEW IF EXISTS mm_tournaments_participation#
-- CREATE VIEW mm_tournaments_participation AS
-- SELECT
--   lu.PROVIDER_NAME AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,tvt.GAME_TYPE AS GAME_TYPE,
--   unix_timestamp(ts.TOURNAMENT_FINISHED_TS) AS LAST_TIMESTAMP
-- FROM
--   strataprod.TOURNAMENT_SUMMARY ts
--   JOIN strataprod.TOURNAMENT_PLAYER tp
--   JOIN strataprod.TOURNAMENT t
--   JOIN strataprod.TOURNAMENT_VARIATION_TEMPLATE tvt
--   JOIN strataprod.PLAYER p
--   JOIN strataprod.LOBBY_USER lu
-- WHERE
--   ts.TOURNAMENT_ID=tp.TOURNAMENT_ID AND tp.TOURNAMENT_ID=t.TOURNAMENT_ID AND t.TOURNAMENT_VARIATION_TEMPLATE_ID=tvt.TOURNAMENT_VARIATION_TEMPLATE_ID
--   AND tp.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID
-- #

DROP PROCEDURE IF EXISTS mm_export_latest_tournaments#
CREATE PROCEDURE mm_export_latest_tournaments(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,GAME_TYPE,LAST_TIMESTAMP ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @lastId = NULL;
  SELECT LAST_ID FROM rpt_mm_export_status WHERE ID='TOURNAMENTS' INTO @lastId;

  IF (@lastId IS NULL) THEN
    SET @request_clauses = ' FROM mm_tournaments_participation';
  ELSE
    SET @request_clauses = CONCAT(' FROM mm_tournaments_participation WHERE LAST_TIMESTAMP > ', @lastId);
  END IF;

  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#
