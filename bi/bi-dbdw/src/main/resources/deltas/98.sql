/********************************************************************************
 **      ACCOUNT                                                               **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`ACCOUNT`#

CREATE TABLE `strataproddw`.`ACCOUNT` (
  `ACCOUNT_ID` bigint(11) NOT NULL,
  `BALANCE` decimal(64,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`ACCOUNT_ID`)
)#


/********************************************************************************
 **      LOBBY_USER                                                            **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`LOBBY_USER`#

CREATE TABLE `strataproddw`.`LOBBY_USER` (
  `PLAYER_ID` bigint(11) NOT NULL,
  `USER_ID` bigint(11) NOT NULL,
  `TSREG` timestamp NULL DEFAULT NULL,
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  `REAL_NAME` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) DEFAULT NULL,
  `PICTURE_LOCATION` varchar(255) DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `COUNTRY` varchar(3) DEFAULT NULL,
  `EXTERNAL_ID` varchar(255) DEFAULT NULL,
  `PROVIDER_NAME` varchar(255) NOT NULL,
  `RPX_PROVIDER` varchar(255) NOT NULL,
  `BLOCKED` tinyint(1) NOT NULL DEFAULT '0',
  `DATE_OF_BIRTH` date DEFAULT NULL,
  `GENDER` varchar(1) DEFAULT NULL,
  `REFERRAL_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PLAYER_ID`),
  KEY `USER_ID_IDX` (`USER_ID`)
)#

DROP VIEW IF EXISTS `strataproddw`.`YAZINO_LOGIN`#

CREATE VIEW `strataproddw`.`YAZINO_LOGIN`
AS
  SELECT `lu`.`USER_ID` AS `USER_ID`,
         `lu`.`EMAIL_ADDRESS` AS `EMAIL_ADDRESS`
  FROM `strataproddw`.`LOBBY_USER` `lu`#


/********************************************************************************
 **      PLAYER_LEVEL                                                          **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`PLAYER_LEVEL`#

CREATE TABLE `strataproddw`.`PLAYER_LEVEL` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `GAME_TYPE` varchar(255) NOT NULL,
  `LEVEL`     int(11) NOT NULL,
  PRIMARY KEY (`PLAYER_ID`, `GAME_TYPE`)
)#



/********************************************************************************
 **      PLAYER_DEFINITION + PLAYER                                            **
 ********************************************************************************/


DROP TABLE IF EXISTS `strataproddw`.`PLAYER_DEFINITION`#

CREATE TABLE `strataproddw`.`PLAYER_DEFINITION` (
  `PLAYER_ID` bigint(11) NOT NULL,
  `TSCREATED` timestamp NULL DEFAULT NULL,
  `ACCOUNT_ID` int(11) DEFAULT NULL,
  `IS_INSIDER` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`PLAYER_ID`)
)#


DROP TABLE IF EXISTS `strataproddw`.`PLAYER`#
DROP VIEW IF EXISTS `strataproddw`.`PLAYER`#

CREATE OR REPLACE VIEW `strataproddw`.`PLAYER` AS
  SELECT `U`.`PLAYER_ID` AS `PLAYER_ID`,
        `U`.`USER_ID` AS `USER_ID`,
        `P`.`ACCOUNT_ID` AS `ACCOUNT_ID`,
        `P`.`TSCREATED` AS `TSCREATED`,
        `P`.`IS_INSIDER` AS `IS_INSIDER`,
        `U`.`PICTURE_LOCATION` AS `PICTURE_LOCATION`,
        GROUP_CONCAT(CONCAT(`L`.`GAME_TYPE`, '\t', `L`.`LEVEL`, '\t0') SEPARATOR '\n') as `LEVEL`
  FROM `strataproddw`.`PLAYER_DEFINITION` `P`,
        `strataproddw`.`LOBBY_USER` `U` LEFT JOIN
        `strataproddw`.`PLAYER_LEVEL` `L` ON  `U`.`PLAYER_ID`=`L`.`PLAYER_ID`
  WHERE `P`.`PLAYER_ID`=`U`.`PLAYER_ID` GROUP BY `P`.`PLAYER_ID`#






/********************************************************************************
 **      GAME_VARIATION_TEMPLATE                                               **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`GAME_VARIATION_TEMPLATE`#

CREATE TABLE `strataproddw`.`GAME_VARIATION_TEMPLATE` (
  `GAME_VARIATION_TEMPLATE_ID` bigint(11) NOT NULL,
  `GAME_TYPE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`GAME_VARIATION_TEMPLATE_ID`)
)#

CREATE OR REPLACE VIEW `strataproddw`.`GAME_TYPE`
AS
  SELECT  DISTINCT `V`.`GAME_TYPE` AS `GAME_TYPE`
  FROM `strataproddw`.`GAME_VARIATION_TEMPLATE` `V`#




/********************************************************************************
 **      TABLE_DEFINITION + TABLE_INFO                                         **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TABLE_DEFINITION`#

CREATE TABLE `strataproddw`.`TABLE_DEFINITION` (
  `TABLE_ID` bigint(11) NOT NULL,
  `GAME_VARIATION_TEMPLATE_ID` bigint(11) NOT NULL,
  PRIMARY KEY (`TABLE_ID`)
)#


DROP TABLE IF EXISTS `strataproddw`.`TABLE_INFO`#

CREATE OR REPLACE VIEW `strataproddw`.`TABLE_INFO`
AS
  SELECT `T`.`TABLE_ID` AS `TABLE_ID`,
         CONCAT(`V`.`NAME`,' ',`T`.`TABLE_ID`) AS `TABLE_NAME`,
         `V`.`GAME_TYPE` AS `GAME_TYPE`,
         `T`.`GAME_VARIATION_TEMPLATE_ID` AS `GAME_VARIATION_TEMPLATE_ID`
  FROM `strataproddw`.`TABLE_DEFINITION` `T`,
        `strataproddw`.`GAME_VARIATION_TEMPLATE` `V`
  WHERE `T`.`GAME_VARIATION_TEMPLATE_ID`=`V`.`GAME_VARIATION_TEMPLATE_ID`#




/********************************************************************************
 **      LEADERBOARD                                                           **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`LEADERBOARD`#

CREATE TABLE `strataproddw`.`LEADERBOARD` (
  `LEADERBOARD_ID` bigint(11) NOT NULL,
  `GAME_TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`LEADERBOARD_ID`)
)#




/********************************************************************************
 **      LEADERBOARD_POSITION                                                  **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`LEADERBOARD_POSITION`#

CREATE TABLE `strataproddw`.`LEADERBOARD_POSITION` (
  `LEADERBOARD_ID` bigint(11) NOT NULL,
  `PLAYER_ID` bigint(11) DEFAULT NULL,
  `LEADERBOARD_POSITION` int(11) DEFAULT NULL
)#




/********************************************************************************
 **      TOURNAMENT                                                            **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TOURNAMENT`#

CREATE TABLE `strataproddw`.`TOURNAMENT` (
  `TOURNAMENT_ID` bigint(20) NOT NULL,
  `TOURNAMENT_START_TS` timestamp NOT NULL,
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(11) NOT NULL,
  PRIMARY KEY (`TOURNAMENT_ID`)
)#




/********************************************************************************
 **      TOURNAMENT_SUMMARY                                                    **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TOURNAMENT_SUMMARY`#

CREATE TABLE `strataproddw`.`TOURNAMENT_SUMMARY` (
	`TOURNAMENT_ID` bigint(20) NOT NULL,
	`TOURNAMENT_NAME` varchar(255) DEFAULT NULL,
	`TOURNAMENT_FINISHED_TS` timestamp NULL DEFAULT NULL
)#




/********************************************************************************
 **      TOURNAMENT_PLAYER                                                     **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TOURNAMENT_PLAYER`#

CREATE TABLE `strataproddw`.`TOURNAMENT_PLAYER` (
	`TOURNAMENT_ID` bigint(20) NOT NULL,
	`PLAYER_ID` bigint(20) NOT NULL
)#




/********************************************************************************
 **      TOURNAMENT_PLAYER_SUMMARY                                             **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TOURNAMENT_PLAYER_SUMMARY`#

CREATE TABLE `strataproddw`.`TOURNAMENT_PLAYER_SUMMARY` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `TOURNAMENT_ID` bigint(11) NOT NULL,
  `POSITION` int NOT NULL,
  `PRIZE` bigint(11) NOT NULL,
  PRIMARY KEY (`PLAYER_ID`, `TOURNAMENT_ID`)
)#




/********************************************************************************
 **      TOURNAMENT_VARIATION_TEMPLATE                                         **
 ********************************************************************************/

DROP TABLE IF EXISTS `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE`#

CREATE TABLE `strataproddw`.`TOURNAMENT_VARIATION_TEMPLATE` (
	`TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL,
	`GAME_TYPE` varchar(255) NOT NULL,
	PRIMARY KEY (`TOURNAMENT_VARIATION_TEMPLATE_ID`)
)#
