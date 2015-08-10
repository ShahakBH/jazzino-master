DROP VIEW IF EXISTS mm_last_leaderboard#
CREATE VIEW mm_last_leaderboard AS
SELECT LEADERBOARD_ID,GAME_TYPE FROM strataprod.LEADERBOARD WHERE NOT END_TS IS NULL ORDER BY END_TS DESC
#

-- DROP VIEW IF EXISTS mm_leaderboard_state#
-- CREATE VIEW mm_leaderboard_state AS
-- SELECT
--   lu.PROVIDER_NAME AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,lastBoard.GAME_TYPE AS GAME_TYPE,lbp.LEADERBOARD_POSITION AS BOARD_GRADE
-- FROM
--   mm_last_leaderboard lastBoard
--   JOIN strataprod.LEADERBOARD_PLAYER lbp
--   JOIN strataprod.PLAYER p
--   JOIN strataprod.LOBBY_USER lu
-- WHERE
--   lastBoard.LEADERBOARD_ID=lbp.LEADERBOARD_ID AND lbp.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID
-- #

DROP PROCEDURE IF EXISTS mm_export_latest_leaderboard#
CREATE PROCEDURE mm_export_latest_leaderboard(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,GAME_TYPE,BOARD_GRADE ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @request_clauses = ' FROM mm_leaderboard_state';
  
  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#