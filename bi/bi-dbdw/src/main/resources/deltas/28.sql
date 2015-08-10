DROP VIEW IF EXISTS mm_initial_games#
-- CREATE VIEW mm_initial_games AS
-- SELECT lu.PROVIDER_NAME AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,lp.GAME_TYPE AS GAME_TYPE, lp.LAST_PLAYED AS LAST_TS
-- FROM
--   LAST_PLAYED lp
--   JOIN strataprod.PLAYER p
--   JOIN strataprod.LOBBY_USER lu
-- WHERE lp.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID
-- #

DROP PROCEDURE IF EXISTS mm_export_initial_games#
CREATE PROCEDURE mm_export_initial_games(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,GAME_TYPE,LAST_TS ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @request_clauses = ' FROM mm_initial_games';
  
  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#