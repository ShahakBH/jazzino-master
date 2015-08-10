DROP VIEW IF EXISTS mm_logins#
-- CREATE VIEW mm_logins AS
-- SELECT l.PROVIDER_NAME PARTNER_ID,l.EXTERNAL_ID EXTERNAL_ID,l.USER_ID USER_ID,
--   s.IP_ADDRESS IP_ADDRESS,
--   s.referer SOURCE,s.tsstarted LOGIN_TIME
-- FROM ACCOUNT_SESSION s
-- JOIN strataprod.PLAYER p ON s.ACCOUNT_ID=p.ACCOUNT_ID
-- JOIN strataprod.LOBBY_USER l ON l.USER_ID=p.USER_ID
-- ORDER BY s.tsstarted#

DROP PROCEDURE IF EXISTS mm_export_latest_logins#
CREATE PROCEDURE mm_export_latest_logins(filename VARCHAR(1024), shiftStart BIGINT, shiftEnd BIGINT)
BEGIN
  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,IP_ADDRESS,SOURCE,LOGIN_TIME ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @request_clauses = CONCAT(' FROM mm_logins WHERE LOGIN_TIME BETWEEN date(date_sub(now(), interval ', shiftStart, 
    ' DAY)) AND date(date_sub(now(), interval ', shiftEnd, ' DAY))');
  
  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#
    
DROP VIEW IF EXISTS mm_chips#
-- CREATE VIEW mm_chips AS
-- SELECT l.PROVIDER_NAME PARTNER_ID,l.EXTERNAL_ID EXTERNAL_ID,l.USER_ID USER_ID,a.BALANCE BALANCE
-- FROM strataprod.ACCOUNT a
-- JOIN strataprod.PLAYER p ON a.ACCOUNT_ID=p.ACCOUNT_ID
-- JOIN strataprod.LOBBY_USER l ON l.USER_ID=p.USER_ID#

DROP PROCEDURE IF EXISTS mm_export_latest_chips#
CREATE PROCEDURE mm_export_latest_chips(filename VARCHAR(1024))
BEGIN  
  SET @request_body = 'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,BALANCE ';
  SET @request_export_conditions = CONCAT(
    'INTO OUTFILE \'', filename, '\'',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' '
  );

  SET @request_clauses = ' FROM mm_chips';
  
  SET @selstatement = CONCAT(@request_body, @request_export_conditions, @request_clauses);
  PREPARE tmp_stmt FROM @selstatement;
  EXECUTE tmp_stmt;
  DEALLOCATE PREPARE tmp_stmt;
END#