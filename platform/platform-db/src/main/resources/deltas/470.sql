-- WEB-3953 - fix TABLE_ID type

-- This is to get around MySQL varying the case of constraints across OSs
DROP PROCEDURE IF EXISTS drop_table_constraints#
CREATE PROCEDURE drop_table_constraints()
  BEGIN
    DECLARE curr_table_name VARCHAR(100);
    DECLARE curr_constraint_name VARCHAR(100);
    DECLARE done BOOLEAN DEFAULT false;

    DECLARE fk_cursor CURSOR FOR SELECT table_name,constraint_name FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'strataprod' AND TABLE_NAME = 'TABLE_GAME_VARIATION' AND COLUMN_NAME='TABLE_ID' AND REFERENCED_TABLE_NAME IS NOT NULL;
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

CALL drop_table_constraints()#
DROP PROCEDURE IF EXISTS drop_table_constraints#

ALTER TABLE TABLE_INFO
  MODIFY TABLE_ID DECIMAL(16,2) NOT NULL#

ALTER TABLE TABLE_INVITE
  MODIFY TABLE_ID DECIMAL(16,2) NOT NULL#

ALTER TABLE TOURNAMENT_TABLE
  MODIFY TABLE_ID DECIMAL(16,2) NOT NULL#

ALTER TABLE TABLE_GAME_VARIATION
  MODIFY TABLE_ID DECIMAL(16,2) NOT NULL,
  ADD CONSTRAINT FK_TABLE_GAME_VARIATION_TABLE_INFO FOREIGN KEY (TABLE_ID) REFERENCES TABLE_INFO (TABLE_ID)#

