/********************************************************************************
 **      TOURNAMENT_SUMMARY                                                    **
 ********************************************************************************/

DELETE FROM `strataproddw`.`TOURNAMENT_SUMMARY`#

INSERT INTO `strataproddw`.`TOURNAMENT_SUMMARY`(`TOURNAMENT_ID`, `TOURNAMENT_NAME`, `TOURNAMENT_FINISHED_TS`)
    SELECT `TS`.`TOURNAMENT_ID`, `TS`.`TOURNAMENT_NAME`, `TS`.`TOURNAMENT_FINISHED_TS` 
    FROM `strataprod`.`TOURNAMENT_SUMMARY` `TS`#
    

/********************************************************************************
 **      TOURNAMENT_PLAYER                                                     **
 ********************************************************************************/

DELETE FROM `strataproddw`.`TOURNAMENT_PLAYER`#

INSERT INTO `strataproddw`.`TOURNAMENT_PLAYER`(`TOURNAMENT_ID`, `PLAYER_ID`)
        SELECT `TP`.`TOURNAMENT_ID`, `TP`.`PLAYER_ID`
        FROM `strataprod`.`TOURNAMENT_PLAYER` `TP`#

