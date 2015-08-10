-- WEB-3678 - Drop the Account Statement foreign key

-- This is to get around MySQL varying the case of constraints across OSs
DROP PROCEDURE IF EXISTS drop_account_stmt_constraints#
CREATE PROCEDURE drop_account_stmt_constraints()
BEGIN
  DECLARE curr_table_name VARCHAR(100);
  DECLARE curr_constraint_name VARCHAR(100);
  DECLARE done BOOLEAN DEFAULT false;

  DECLARE fk_cursor CURSOR FOR SELECT table_name,constraint_name FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'strataprod' AND TABLE_NAME = 'ACCOUNT_STATEMENT' AND COLUMN_NAME='ACCOUNT_ID' AND REFERENCED_TABLE_NAME IS NOT NULL;
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

CALL drop_account_stmt_constraints()#
DROP PROCEDURE IF EXISTS drop_account_stmt_constraints#

ALTER TABLE ACCOUNT_STATEMENT DROP KEY FK_ACC_STMT_ACC#
