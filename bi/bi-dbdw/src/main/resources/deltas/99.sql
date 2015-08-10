/********************************************************************************
 **      ACCOUNT                                                               **
 ********************************************************************************/

/** Migration Script */
DELETE FROM `strataproddw`.`ACCOUNT`#
INSERT INTO `strataproddw`.`ACCOUNT`(`ACCOUNT_ID`, `BALANCE`)
  SELECT `ACCOUNT_ID`, `BALANCE` FROM `strataprod`.`ACCOUNT`#


/********************************************************************************
 **      LOBBY_USER                                                            **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`LOBBY_USER`#

INSERT INTO `strataproddw`.`LOBBY_USER` (`PLAYER_ID`, `USER_ID`, `TSREG`,`DISPLAY_NAME`,`REAL_NAME`,`FIRST_NAME`,`EMAIL_ADDRESS`,`COUNTRY`,`EXTERNAL_ID`,`PROVIDER_NAME`,`RPX_PROVIDER`,`DATE_OF_BIRTH`,`GENDER`,`REFERRAL_ID`)
  SELECT `PLAYER_ID`,`PLAYER_ID`, `TSREG`,`DISPLAY_NAME`,`REAL_NAME`,`FIRST_NAME`,`EMAIL_ADDRESS`,`COUNTRY`,`EXTERNAL_ID`,`PROVIDER_NAME`,`RPX_PROVIDER`,`DATE_OF_BIRTH`,`GENDER`,`REFERRAL_ID`
  FROM `strataprod`.`LOBBY_USER`
  WHERE `PLAYER_ID` IS NOT NULL#




/********************************************************************************
 **      PLAYER_DEFINITION + PLAYER                                            **
 ********************************************************************************/

/** Migration Script */
DELETE FROM `strataproddw`.`PLAYER_DEFINITION`#
INSERT INTO `strataproddw`.`PLAYER_DEFINITION`(`PLAYER_ID`, `TSCREATED`, `ACCOUNT_ID`)
  SELECT `PLAYER_ID`, `TSCREATED`, `ACCOUNT_ID` FROM `strataprod`.`PLAYER`#




/********************************************************************************
 **      PLAYER_LEVEL                                                          **
 ********************************************************************************/

/** Migration Script */

DELETE FROM `strataproddw`.`PLAYER_LEVEL`#
DROP PROCEDURE IF EXISTS `strataproddw`.`migrate_player_levels`#
CREATE PROCEDURE `strataproddw`.`migrate_player_levels`()
BEGIN
  DECLARE player_id_val BIGINT(20);
  DECLARE lvl TEXT;
  DECLARE txt TEXT;
  DECLARE i INT;
  DECLARE line TEXT;
  DECLARE gameType VARCHAR(50);
  DECLARE gameLevel VARCHAR(5);
  DECLARE gameXP VARCHAR(50);
  DECLARE t1,t2 INT;
  DECLARE done INT DEFAULT 0;
  DECLARE c CURSOR FOR SELECT `PLAYER_ID`, `LEVEL` FROM strataprod.PLAYER;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN c;

  read_loop: LOOP
    FETCH c INTO player_id_val, lvl;
    IF done THEN
      LEAVE read_loop;
    END IF;
    IF lvl IS NOT NULL THEN
      -- parse the level info
      -- loop over each line of the level info and parse out the data
      -- extract for each line:  game /t level /t XP
      SET txt = lvl;

      WHILE LENGTH(txt) > 0 DO
        SET i = LOCATE("\n", txt);
        IF i > 0 THEN
          SET line = LEFT(txt, i);
        ELSE
          SET line = txt;
        END IF;

        -- Parse out the pieces
        SET t1 = LOCATE("\t", line);
        SET t2 = LOCATE("\t", line, t1 + 1);
        SET gameType = LEFT(line, t1 - 1);
        SET gameLevel = MID(line, t1 + 1, t2 - t1 -1);
        SET gameXP = MID(line, t2 + 1, LENGTH(line) - t2);

        INSERT INTO PLAYER_LEVEL (PLAYER_ID, GAME_TYPE, LEVEL)
        VALUES (player_id_val, gameType, gameLevel);

        -- Chop off current line and move on to whatever is left
        SET txt = RIGHT(txt, LENGTH(txt) - LENGTH(line));
      END WHILE;
    END IF;
  END LOOP;

  CLOSE c;
END
#


call `strataproddw`.`migrate_player_levels`#




/********************************************************************************
 **      GAME_VARIATION_TEMPLATE                                               **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`GAME_VARIATION_TEMPLATE`#

INSERT INTO `strataproddw`.`GAME_VARIATION_TEMPLATE` (`GAME_VARIATION_TEMPLATE_ID`, `GAME_TYPE`, `NAME`)
  SELECT `V`.`GAME_VARIATION_TEMPLATE_ID`, `V`.`GAME_TYPE`, `V`.`NAME`
  FROM `strataprod`.`GAME_VARIATION_TEMPLATE` `V`#




/********************************************************************************
 **      TABLE_DEFINITION + TABLE_INFO                                         **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`TABLE_DEFINITION`#

INSERT INTO `strataproddw`.`TABLE_DEFINITION` (`TABLE_ID`, `GAME_VARIATION_TEMPLATE_ID`)
  SELECT `T`.`TABLE_ID`,  `T`.`GAME_VARIATION_TEMPLATE_ID`
  FROM `strataprod`.`TABLE_INFO` `T`#




/********************************************************************************
 **      LEADERBOARD                                                           **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`LEADERBOARD`#

INSERT INTO `strataproddw`.`LEADERBOARD` (`LEADERBOARD_ID`, `GAME_TYPE`)
  SELECT `L`.`LEADERBOARD_ID`, `L`.`GAME_TYPE`
  FROM `strataprod`.`LEADERBOARD` `L`#




/********************************************************************************
 **      LEADERBOARD_POSITION                                                  **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`LEADERBOARD_POSITION`#

INSERT INTO `strataproddw`.`LEADERBOARD_POSITION` (`LEADERBOARD_ID`, `PLAYER_ID`, `LEADERBOARD_POSITION`)
  SELECT `L`.`LEADERBOARD_ID`, `L`.`PLAYER_ID`, `L`.`LEADERBOARD_POSITION`
  FROM `strataprod`.`LEADERBOARD_RESULT` L
  WHERE RESULT_TS = (
    SELECT MAX(RESULT_TS)
    FROM `strataprod`.`LEADERBOARD_RESULT`  P
    WHERE `P`.`PLAYER_ID` = `L`.`PLAYER_ID` AND `P`.`LEADERBOARD_ID` = `L`.`LEADERBOARD_ID`
  )#




/********************************************************************************
 **      TOURNAMENT                                                            **
 ********************************************************************************/

/** Migration Script */
DELETE FROM `strataproddw`.`TOURNAMENT`#

INSERT INTO `strataproddw`.`TOURNAMENT`(`TOURNAMENT_ID`, `TOURNAMENT_START_TS`, `TOURNAMENT_VARIATION_TEMPLATE_ID`)
  SELECT `T`.`TOURNAMENT_ID`, `T`.`TOURNAMENT_START_TS`, `T`.`TOURNAMENT_VARIATION_TEMPLATE_ID`
  FROM   `strataprod`.`TOURNAMENT` `T`#




/********************************************************************************
 **      TOURNAMENT_SUMMARY                                                    **
 ********************************************************************************/





/********************************************************************************
 **      TOURNAMENT_PLAYER                                                     **
 ********************************************************************************/





/********************************************************************************
 **      TOURNAMENT_PLAYER_SUMMARY                                             **
 ********************************************************************************/


/** Migration Script **/
DELETE FROM `strataproddw`.`TOURNAMENT_PLAYER_SUMMARY`#

INSERT INTO `strataproddw`.`TOURNAMENT_PLAYER_SUMMARY` (`PLAYER_ID`, `TOURNAMENT_ID`, `POSITION`, `PRIZE`)
  SELECT `TP`.`PLAYER_ID`, `TP`.`TOURNAMENT_ID`, MAX(`TP`.`LEADERBOARD_POSITION`), MAX(`TP`.`SETTLED_PRIZE`)
    FROM `strataprod`.`TOURNAMENT_PLAYER` `TP` GROUP BY `TP`.`PLAYER_ID`, `TP`.`TOURNAMENT_ID`#




/********************************************************************************
 **      TOURNAMENT_VARIATION_TEMPLATE                                         **
 ********************************************************************************/

/** Migration Script **/
DELETE FROM `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE`#

INSERT INTO `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE` (`TOURNAMENT_VARIATION_TEMPLATE_ID`, `GAME_TYPE`)
  SELECT `V`.`TOURNAMENT_VARIATION_TEMPLATE_ID`, `V`.`GAME_TYPE`
  FROM `strataprod`.`TOURNAMENT_VARIATION_TEMPLATE` `V`#

