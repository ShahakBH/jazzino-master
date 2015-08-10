DROP VIEW IF EXISTS mm_total_invites#
-- CREATE VIEW mm_total_invites AS
-- SELECT
--   lu.PROVIDER_NAME AS PARTNER_ID, lu.EXTERNAL_ID AS EXTERNAL_ID,p.USER_ID AS USER_ID,
--   i.STATUS AS STATUS,unix_timestamp(i.CREATED_TS) AS CREATED_TS,unix_timestamp(i.UPDATED_TS) AS UPDATED_TS
-- FROM
--   INVITATIONS i
--   JOIN strataprod.PLAYER p
--   JOIN strataprod.LOBBY_USER lu
-- WHERE
--   i.PLAYER_ID=p.PLAYER_ID AND p.USER_ID=lu.USER_ID
-- ORDER BY i.PLAYER_ID
-- #

DROP PROCEDURE IF EXISTS mm_export_latest_invites#
CREATE PROCEDURE mm_export_latest_invites(filename VARCHAR(1024))
BEGIN
  DECLARE lastId BIGINT;
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,STATUS,CREATED_TS,UPDATED_TS ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @lastId = NULL;
  SELECT LAST_ID FROM rpt_mm_export_status WHERE ID='INVITES' INTO @lastId;

  IF (@lastId IS NULL) THEN
    SET @request_clauses = ' FROM mm_total_invites';
   ELSE
    SET @request_clauses = CONCAT(' FROM mm_total_invites WHERE UPDATED_TS > ', @lastId, ' OR (UPDATED_TS IS NULL AND CREATED_TS > ', @lastId, ')');
  END IF;

  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#
