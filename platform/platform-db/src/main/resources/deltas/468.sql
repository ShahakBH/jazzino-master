-- WEB-3953 - fix ACCOUNT_ID type

-- This is to get around MySQL varying the case of constraints across OSs
DROP PROCEDURE IF EXISTS drop_table_info_account_constraints#
CREATE PROCEDURE drop_table_info_account_constraints()
  BEGIN
    DECLARE curr_table_name VARCHAR(100);
    DECLARE curr_constraint_name VARCHAR(100);
    DECLARE done BOOLEAN DEFAULT false;

    DECLARE fk_cursor CURSOR FOR SELECT table_name,constraint_name FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'strataprod' AND TABLE_NAME = 'TABLE_INFO' AND COLUMN_NAME='TABLE_ACCOUNT_ID' AND REFERENCED_TABLE_NAME IS NOT NULL;
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

CALL drop_table_info_account_constraints()#
DROP PROCEDURE IF EXISTS drop_table_info_account_constraints#

ALTER TABLE TOURNAMENT
  DROP KEY FK_TOURNAMENT_ACCOUNT,
  DROP KEY FK_TOURNAMENT_POT_ACCOUNT,
  DROP FOREIGN KEY FK_TOURNAMENT_ACCOUNT,
  DROP FOREIGN KEY FK_TOURNAMENT_POT_ACCOUNT#

ALTER TABLE ACCOUNT
  DROP FOREIGN KEY FK_ACCOUNT_PARENT_ACCOUNT,
  MODIFY ACCOUNT_ID DECIMAL(16,2) NOT NULL,
  MODIFY PARENT_ACCOUNT_ID DECIMAL(16,2) DEFAULT NULL#

ALTER TABLE ACCOUNT
  ADD CONSTRAINT FK_ACCOUNT_PARENT_ACCOUNT FOREIGN KEY (PARENT_ACCOUNT_ID) REFERENCES ACCOUNT (ACCOUNT_ID)#

ALTER TABLE ACCOUNT_STATEMENT
  MODIFY ACCOUNT_ID DECIMAL(16,2) NOT NULL#

ALTER TABLE PLAYER
  MODIFY ACCOUNT_ID DECIMAL(16,2) NOT NULL#

ALTER TABLE TOURNAMENT
  MODIFY TOURNAMENT_ACCOUNT_ID DECIMAL(16,2) DEFAULT NULL,
  MODIFY TOURNAMENT_POT_ACCOUNT_ID DECIMAL(16,2) DEFAULT NULL,
  ADD CONSTRAINT FK_TOURNAMENT_ACCOUNT FOREIGN KEY (TOURNAMENT_ACCOUNT_ID) REFERENCES ACCOUNT (ACCOUNT_ID),
  ADD CONSTRAINT FK_TOURNAMENT_POT_ACCOUNT FOREIGN KEY (TOURNAMENT_POT_ACCOUNT_ID) REFERENCES ACCOUNT (ACCOUNT_ID)#

ALTER TABLE TABLE_INFO
  MODIFY TABLE_ACCOUNT_ID DECIMAL(16,2) DEFAULT NULL,
  ADD CONSTRAINT TABLE_INFO_ACCOUNT FOREIGN KEY (TABLE_ACCOUNT_ID) REFERENCES ACCOUNT (ACCOUNT_ID)#

ALTER TABLE TOURNAMENT_PLAYER
  MODIFY TOURNAMENT_ACCOUNT_ID DECIMAL(16,2) NOT NULL#
