DROP PROCEDURE IF EXISTS export_dynamic;
DELIMITER $
CREATE PROCEDURE export_dynamic() 
BEGIN 
  set @selstatement = CONCAT(
    'SELECT PARTNER_ID,EXTERNAL_ID,USER_ID,LEVEL ',
    'INTO OUTFILE \'/tmp/game-info-input', (CURDATE()+0), '.csv\' ',
    'FIELDS TERMINATED BY \',\' ENCLOSED BY \'"\' ESCAPED BY \'\\\\\' ',
    'LINES TERMINATED BY ''\\n\' ',
    'FROM PLAYER'
  ) ;
  PREPARE stmt1 FROM @selstatement; 
  EXECUTE stmt1; 
  Deallocate prepare stmt1; 
END;
$
DELIMITER ;
CALL export_dynamic();
DROP PROCEDURE export_dynamic;