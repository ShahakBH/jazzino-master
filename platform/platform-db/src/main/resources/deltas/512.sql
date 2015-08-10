-- WEB-4773 removing link to partner table
-- This is to get around MySQL varying the case of constraints across OSs

DROP PROCEDURE IF EXISTS drop_partner_constraints#
CREATE PROCEDURE drop_partner_constraints()
BEGIN
  DECLARE curr_table_name VARCHAR(100);
  DECLARE curr_constraint_name VARCHAR(100);
  DECLARE done BOOLEAN DEFAULT false;

  DECLARE fk_cursor CURSOR FOR SELECT table_name,constraint_name FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'strataprod'
   AND TABLE_NAME in ('LOBBY_USER','TABLE_INFO') AND COLUMN_NAME = 'PARTNER_ID' AND REFERENCED_TABLE_NAME IS NOT NULL;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = true;

  OPEN fk_cursor;
  cursor_loop: LOOP
    FETCH fk_cursor INTO curr_table_name,curr_constraint_name;
    IF done THEN
      LEAVE cursor_loop;
    END IF;

    SET @query = CONCAT('alter table ',curr_table_name,' drop foreign key ',curr_constraint_name);

    PREPARE stmt FROM @query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END LOOP;

  CLOSE fk_cursor;
END#

CALL drop_partner_constraints()#
DROP PROCEDURE IF EXISTS drop_partner_constraints#


alter table LOBBY_USER alter column PARTNER_ID set default 'YAZINO'#

update LOBBY_USER set PARTNER_ID ='YAZINO' where PARTNER_ID='PLAY_FOR_FUN'#

