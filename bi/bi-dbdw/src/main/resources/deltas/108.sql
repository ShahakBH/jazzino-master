  SELECT `U`.`PLAYER_ID` AS `PLAYER_ID`,
        `U`.`USER_ID` AS `USER_ID`,
        `P`.`ACCOUNT_ID` AS `ACCOUNT_ID`,
        `P`.`TSCREATED` AS `TSCREATED`,
        `P`.`IS_INSIDER` AS `IS_INSIDER`,
        `U`.`PICTURE_LOCATION` AS `PICTURE_LOCATION`
  FROM `strataproddw`.`PLAYER_DEFINITION` `P`,
        `strataproddw`.`LOBBY_USER` `U`
  WHERE `P`.`PLAYER_ID`=`U`.`PLAYER_ID` GROUP BY `P`.`PLAYER_ID`
#