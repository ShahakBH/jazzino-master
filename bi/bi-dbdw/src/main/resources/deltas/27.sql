-- DROP VIEW IF EXISTS mm_last_levels#
-- CREATE VIEW mm_last_levels AS
-- SELECT
--   lu.PROVIDER_NAME AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,p.LEVEL AS LEVEL,lp.LAST_PLAYED AS LAST_PLAYED
-- FROM
--   LAST_PLAYED lp
--   JOIN strataprod.PLAYER p
--   JOIN strataprod.LOBBY_USER lu
-- WHERE
--   lp.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID AND NOT p.LEVEL IS NULL
-- #

DROP PROCEDURE IF EXISTS mm_export_latest_levels#
CREATE PROCEDURE mm_export_latest_levels(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,LEVEL ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @request_clauses = ' FROM mm_last_levels';
  
  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#