-- WEB-4173 - drop MAXIMILES_ID (aka USER_ID, aka PLAYER_PROFILE_ID)

-- MySQL won't let us change the primary key while there are FK constraints on the new key.
-- This is to get around MySQL varying the case of constraints across OSs
DROP PROCEDURE IF EXISTS drop_yazino_login_player_constraints#
CREATE PROCEDURE drop_yazino_login_player_constraints()
BEGIN
  DECLARE curr_table_name VARCHAR(100);
  DECLARE curr_constraint_name VARCHAR(100);
  DECLARE done BOOLEAN DEFAULT false;

  DECLARE fk_cursor CURSOR FOR SELECT table_name,constraint_name FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'strataprod' AND TABLE_NAME = 'YAZINO_LOGIN' AND COLUMN_NAME='PLAYER_ID' AND REFERENCED_TABLE_NAME IS NOT NULL;
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

CALL drop_yazino_login_player_constraints()#
DROP PROCEDURE IF EXISTS drop_yazino_login_player_constraints#

ALTER TABLE LOBBY_USER
    CHANGE USER_ID USER_ID BIGINT(20),
    DROP PRIMARY KEY#

-- MakeMyCasino has phantom duplicates (i.e. they break the primary key, but damned if we can find them)
ALTER IGNORE TABLE LOBBY_USER ADD PRIMARY KEY(PLAYER_ID)#

ALTER TABLE LOBBY_USER DROP COLUMN USER_ID#

-- Restore the key we removed for the primary key swap
ALTER TABLE YAZINO_LOGIN ADD CONSTRAINT FK_YAZINO_LOGIN_PLAYER_LOBBY_USER FOREIGN KEY (PLAYER_ID) REFERENCES LOBBY_USER (PLAYER_ID)#
