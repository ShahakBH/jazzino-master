-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `$CONFIGURATION` (
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `$SEQUENCE` (
  `TSALLOCATED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `$STATUS` (
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ACCOUNT` (
  `ACCOUNT_ID` int(11) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `BALANCE` decimal(64,4) NOT NULL DEFAULT '0.0000',
  `VERSION` bigint(20) NOT NULL DEFAULT '0',
  `CREDIT_LIMIT` decimal(64,4) NOT NULL DEFAULT '0.0000',
  `OPEN` int(11) NOT NULL DEFAULT '1',
  `PARENT_ACCOUNT_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`ACCOUNT_ID`),
  KEY `FK_ACCOUNT_PARENT_ACCOUNT` (`PARENT_ACCOUNT_ID`),
  CONSTRAINT `FK_ACCOUNT_PARENT_ACCOUNT` FOREIGN KEY (`PARENT_ACCOUNT_ID`) REFERENCES `ACCOUNT` (`ACCOUNT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ACCOUNT_STATEMENT` (
  `account_statement_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `INTERNAL_TRANSACTION_ID` varchar(255) NOT NULL,
  `ACCOUNT_ID` int(11) NOT NULL,
  `CASHIER_NAME` varchar(255) NOT NULL,
  `GAME_TYPE` varchar(255) DEFAULT NULL,
  `TRANSACTION_STATUS` varchar(255) DEFAULT NULL,
  `PURCHASE_TIMESTAMP` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PURCHASE_CURRENCY` varchar(3) NOT NULL,
  `PURCHASE_AMOUNT` decimal(64,4) NOT NULL,
  `CHIPS_AMOUNT` decimal(64,4) NOT NULL DEFAULT '0.0000',
  PRIMARY KEY (`account_statement_id`),
  UNIQUE KEY `INTERNAL_TRANSACTION_ID` (`INTERNAL_TRANSACTION_ID`),
  KEY `FK_ACC_STMT_ACC` (`ACCOUNT_ID`),
  KEY `PURCHASE_TIMESTAMP` (`PURCHASE_TIMESTAMP`),
  CONSTRAINT `account_statement_ibfk_1` FOREIGN KEY (`ACCOUNT_ID`) REFERENCES `ACCOUNT` (`ACCOUNT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ACHIEVEMENT` (
  `ACHIEVEMENT_ID` varchar(255) NOT NULL,
  `ACHIEVEMENT_TITLE` varchar(255) NOT NULL,
  `POSTED_ACHIEVEMENT_TITLE_TEXT` varchar(255) DEFAULT NULL,
  `POSTED_ACHIEVEMENT_TITLE_LINK` varchar(255) DEFAULT NULL,
  `POSTED_ACHIEVEMENT_ACTION_NAME` varchar(26) DEFAULT NULL,
  `POSTED_ACHIEVEMENT_ACTION_LINK` varchar(255) DEFAULT NULL,
  `ACHIEVEMENT_MESSAGE` varchar(512) NOT NULL,
  `ACHIEVEMENT_SHORT_DESCRIPTION` varchar(255) NOT NULL,
  `EVENT` varchar(255) NOT NULL,
  `ACCUMULATOR` varchar(255) NOT NULL DEFAULT 'singleEvent',
  `ACCUMULATOR_PARAMS` varchar(512) DEFAULT NULL,
  `GAME_TYPE` varchar(255) NOT NULL,
  `ACHIEVEMENT_LEVEL` int(11) NOT NULL,
  `ACHIEVEMENT_HOW_TO_GET` varchar(255) NOT NULL,
  `RECURRING` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`ACHIEVEMENT_ID`),
  KEY `FK_ACHIEVEMENT_GAMETYPE` (`GAME_TYPE`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `BAD_WORD` (
  `WORD` varchar(255) NOT NULL,
  `FIND_PART_WORD` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`WORD`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `CLIENT` (
  `CLIENT_ID` varchar(255) NOT NULL,
  `CLIENT_FILE` varchar(255) NOT NULL,
  `GAME_TYPE` varchar(63) NOT NULL,
  `NUMBER_OF_SEATS` int(11) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `CLIENT_PROPERTY` (
  `CLIENT_ID` varchar(255) NOT NULL,
  `PROPERTY_NAME` varchar(127) NOT NULL,
  `PROPERTY_VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CLIENT_ID`,`PROPERTY_NAME`),
  CONSTRAINT `client_property_ibfk_1` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`CLIENT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `COUNTRY` (
  `ISO_3166_1_CODE` varchar(2) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `CURRENCY_ISO_4217_CODE` varchar(3) NOT NULL DEFAULT 'USD',
  PRIMARY KEY (`ISO_3166_1_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `GAME_CONFIGURATION` (
  `GAME_ID` varchar(255) NOT NULL DEFAULT '',
  `SHORT_NAME` varchar(50) DEFAULT NULL,
  `DISPLAY_NAME` varchar(100) DEFAULT NULL,
  `ALIASES` varchar(255) DEFAULT NULL,
  `ORD` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`GAME_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `GAME_CONFIGURATION_PROPERTY` (
  `GAME_PROPERTY_ID` bigint(64) NOT NULL AUTO_INCREMENT,
  `GAME_ID` varchar(255) DEFAULT NULL,
  `PROPERTY_NAME` varchar(100) DEFAULT NULL,
  `PROPERTY_VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`GAME_PROPERTY_ID`),
  KEY `GAME_ID` (`GAME_ID`),
  CONSTRAINT `game_configuration_property_ibfk_1` FOREIGN KEY (`GAME_ID`) REFERENCES `GAME_CONFIGURATION` (`GAME_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=250 DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `GAME_VARIATION_TEMPLATE` (
  `GAME_VARIATION_TEMPLATE_ID` int(11) NOT NULL AUTO_INCREMENT,
  `GAME_TYPE` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`GAME_VARIATION_TEMPLATE_ID`),
  UNIQUE KEY `UC_GAME_VARIATION_TEMPLATE_GAME_TYPE_NAME` (`GAME_TYPE`,`NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `GAME_VARIATION_TEMPLATE_PROPERTY` (
  `GAME_VARIATION_TEMPLATE_PROPERTY_ID` int(11) NOT NULL AUTO_INCREMENT,
  `GAME_VARIATION_TEMPLATE_ID` int(11) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` text,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`GAME_VARIATION_TEMPLATE_PROPERTY_ID`),
  UNIQUE KEY `UC_VARIATION_TEMPLATE_ID_AND_NAME` (`GAME_VARIATION_TEMPLATE_ID`,`NAME`),
  CONSTRAINT `game_variation_template_property_ibfk_1` FOREIGN KEY (`GAME_VARIATION_TEMPLATE_ID`) REFERENCES `GAME_VARIATION_TEMPLATE` (`GAME_VARIATION_TEMPLATE_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1429 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `INVITATIONS` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `RECIPIENT_IDENTIFIER` varchar(255) NOT NULL,
  `INVITED_FROM` varchar(10) NOT NULL,
  `STATUS` varchar(20) NOT NULL,
  `REWARD` int(11) DEFAULT NULL,
  `CREATED_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `UPDATED_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `GAME_TYPE` varchar(32) DEFAULT NULL,
  `SCREEN_SOURCE` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`PLAYER_ID`,`RECIPIENT_IDENTIFIER`,`INVITED_FROM`),
  KEY `IDX_INVITATIONS_USER_ID` (`PLAYER_ID`),
  KEY `IDX_INVITATIONS_INVITED` (`RECIPIENT_IDENTIFIER`,`INVITED_FROM`),
  KEY `IDX_CREATED_TS` (`CREATED_TS`),
  KEY `IDX_UPDATED_TS` (`UPDATED_TS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `IOS_PLAYER_DEVICE` (
  `GAME_TYPE` varchar(255) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  `DEVICE_TOKEN` varchar(255) NOT NULL,
  `BUNDLE` varchar(30) DEFAULT NULL,
  UNIQUE KEY `uc_bundlePlayerDevice` (`BUNDLE`,`PLAYER_ID`,`DEVICE_TOKEN`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LEADERBOARD` (
  `LEADERBOARD_ID` int(11) NOT NULL,
  `START_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `END_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `CYCLE_LENGTH` bigint(20) DEFAULT NULL,
  `CYCLE_END_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ACTIVE` bit(1) NOT NULL DEFAULT b'1',
  `GAME_TYPE` varchar(255) NOT NULL,
  `POINT_BONUS_PER_PLAYER` int(7) NOT NULL DEFAULT '0',
  `LEADERBOARD_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`LEADERBOARD_ID`),
  KEY `IDX_LEADERBOARD_ACTIVE` (`ACTIVE`),
  KEY `IDX_LEADERBOARD_START` (`START_TS`),
  KEY `IDX_LEADERBOARD_END` (`END_TS`),
  KEY `IDX_LEADERBOARD_GAMETYPE` (`GAME_TYPE`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LEADERBOARD_POSITION` (
  `LEADERBOARD_ID` int(11) NOT NULL,
  `LEADERBOARD_POSITION` int(7) NOT NULL,
  `AWARD_POINTS` int(7) NOT NULL DEFAULT '0',
  `AWARD_PAYOUT` int(7) NOT NULL DEFAULT '0',
  `TROPHY_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`LEADERBOARD_ID`,`LEADERBOARD_POSITION`),
  KEY `FK_LEADERBOARDPOSITION_TROPHY` (`TROPHY_ID`),
  KEY `IDX_LEADERBOARDPOSITION_LEADERBOARD` (`LEADERBOARD_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LEVEL_SYSTEM` (
  `GAME_TYPE` varchar(255) NOT NULL,
  `EXPERIENCE_FACTORS` text,
  `LEVEL_DEFINITIONS` text NOT NULL,
  `VERSION` bigint(20) DEFAULT NULL,
  UNIQUE KEY `UC_GAME_TYPE` (`GAME_TYPE`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LOBBY_USER` (
  `USER_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `UNVERIFIED_EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `PASSWORD_HASH` varchar(255) DEFAULT NULL,
  `REAL_NAME` varchar(255) DEFAULT NULL,
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  `GENDER` varchar(1) DEFAULT NULL,
  `PICTURE_LOCATION` varchar(255) DEFAULT NULL,
  `COUNTRY` varchar(3) DEFAULT NULL,
  `FIRST_NAME` varchar(255) DEFAULT NULL,
  `LAST_NAME` varchar(255) DEFAULT NULL,
  `DATE_OF_BIRTH` date DEFAULT NULL,
  `VERIFICATION_IDENTIFIER` varchar(36) DEFAULT NULL,
  `REFERRAL_ID` varchar(255) DEFAULT NULL,
  `TSREG` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `EXTERNAL_ID` varchar(255) DEFAULT NULL,
  `PROVIDER_NAME` varchar(255) NOT NULL,
  `RPX_PROVIDER` varchar(255) DEFAULT NULL,
  `USER_REGISTRATION_ERROR` varchar(40) DEFAULT NULL,
  `BLOCKED` tinyint(1) NOT NULL DEFAULT '0',
  `PLAYER_ID` bigint(20) DEFAULT NULL,
  `SYNC_PROFILE` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`USER_ID`),
  UNIQUE KEY `IDX_LOBBYUSER_PLAYERID` (`PLAYER_ID`),
  KEY `IDX_USER_UNVERIFIED_EMAIL_ADDRESS` (`UNVERIFIED_EMAIL_ADDRESS`),
  KEY `PROVIDER_NAME` (`PROVIDER_NAME`),
  KEY `IDX_EMAIL_ADDRESS` (`EMAIL_ADDRESS`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PARTNER` (
  `PARTNER_ID` varchar(255) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `INITIAL_BALANCE` bigint(20) NOT NULL DEFAULT '1000',
  `TOPUP_BALANCE` bigint(20) NOT NULL DEFAULT '0',
  `MINIMUM_TOPUP_AMOUNT` bigint(20) NOT NULL DEFAULT '1000',
  `REFERRAL_AMOUNT` decimal(64,2) NOT NULL DEFAULT '0.00',
  `FAN_BALANCE` bigint(20) NOT NULL DEFAULT '2000',
  `MAILINGLIST_BALANCE` bigint(20) NOT NULL DEFAULT '2000',
  `BOOKMARK_BALANCE` bigint(20) NOT NULL DEFAULT '2000',
  PRIMARY KEY (`PARTNER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PAYMENT_STATE` (
  `CASHIER_NAME` varchar(255) NOT NULL,
  `EXTERNAL_TRANSACTION_ID` varchar(255) NOT NULL,
  `STATE` varchar(10) NOT NULL,
  `UPDATED_TS` datetime NOT NULL,
  PRIMARY KEY (`CASHIER_NAME`,`EXTERNAL_TRANSACTION_ID`),
  KEY `UPDATED_TS` (`UPDATED_TS`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PLAYER` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `ACCOUNT_ID` int(11) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `RELATIONSHIPS` mediumtext,
  `PICTURE_LOCATION` varchar(610) DEFAULT NULL,
  `PREFERRED_CURRENCY` varchar(3) DEFAULT NULL,
  `ACHIEVEMENTS` text,
  `REWARDS` varchar(255) DEFAULT NULL,
  `ACHIEVEMENT_PROGRESS` text,
  `LEVEL` text,
  `TSCREATED` timestamp NULL DEFAULT NULL,
  `IS_INSIDER` int(1) DEFAULT NULL,
  `LAST_TOPUP_DATE` datetime DEFAULT NULL,
  `PREFERRED_PAYMENT_METHOD` varchar(20) DEFAULT NULL,
  `ts_last_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PLAYER_ID`),
  KEY `PLAYER_ibfk_2` (`ACCOUNT_ID`),
  KEY `IDX_PLAYER_NAME` (`NAME`),
  KEY `IDX_PLAYER_TSCREATED` (`TSCREATED`),
  KEY `idx_ts_last_update` (`ts_last_update`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `AUTHENTICATION` (
  `PROVIDER` varchar(65) NOT NULL,
  `AUTH_ID` varchar(65) NOT NULL,
  `PASSWORD_HASH` varchar(255) DEFAULT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  `AUTHENTICATION_STATUS` char(1) NOT NULL DEFAULT 'U',
  PRIMARY KEY (`PROVIDER`,`AUTH_ID`),
  KEY `FK_AUTHENTICATION_PLAYER` (`PLAYER_ID`),
  CONSTRAINT `authentication_ibfk_1` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LEADERBOARD_PLAYER` (
  `LEADERBOARD_ID` int(11) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  `LEADERBOARD_POSITION` int(11) NOT NULL,
  `PLAYER_NAME` varchar(255) NOT NULL,
  `PLAYER_POINTS` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`LEADERBOARD_ID`,`PLAYER_ID`),
  KEY `FK_LEADERBOARDPLAYER_PLAYER` (`PLAYER_ID`),
  KEY `IDX_LEADERBOARDPLAYER_LEADERBOARD` (`LEADERBOARD_ID`),
  CONSTRAINT `leaderboard_player_ibfk_1` FOREIGN KEY (`LEADERBOARD_ID`) REFERENCES `LEADERBOARD` (`LEADERBOARD_ID`),
  CONSTRAINT `leaderboard_player_ibfk_2` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `LEADERBOARD_RESULT` (
  `LEADERBOARD_ID` int(11) NOT NULL,
  `RESULT_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PLAYER_ID` bigint(20) NOT NULL,
  `LEADERBOARD_POSITION` int(7) NOT NULL,
  `PLAYER_POINTS` int(11) NOT NULL DEFAULT '0',
  `PLAYER_PAYOUT` int(11) NOT NULL DEFAULT '0',
  `PLAYER_NAME` varchar(255) NOT NULL,
  `EXPIRY_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`LEADERBOARD_ID`,`PLAYER_ID`,`RESULT_TS`),
  KEY `FK_LEADERBOARDRESULT_PLAYER` (`PLAYER_ID`),
  KEY `IDX_LEADERBOARDRESULT_LEADERBOARD` (`LEADERBOARD_ID`),
  KEY `IDX_LEADERBOARDRESULT_RESULTTS` (`RESULT_TS`),
  CONSTRAINT `leaderboard_result_ibfk_1` FOREIGN KEY (`LEADERBOARD_ID`) REFERENCES `LEADERBOARD` (`LEADERBOARD_ID`),
  CONSTRAINT `leaderboard_result_ibfk_2` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PLAYER_INBOX` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PLAYER_ID` bigint(20) NOT NULL,
  `RECEIVED_TIME` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `IS_READ` tinyint(1) NOT NULL DEFAULT '0',
  `MESSAGE` text NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PLAYER_INBOX_UC` (`PLAYER_ID`,`RECEIVED_TIME`),
  KEY `PLAYER_ID` (`PLAYER_ID`),
  CONSTRAINT `PLAYER_INBOX_PLAYER_ID_FK` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PLAYER_PROMOTION_STATUS` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `LAST_TOPUP_DATE` datetime DEFAULT NULL,
  `LAST_PLAYED_DATE` datetime DEFAULT NULL,
  `CONSECUTIVE_PLAY_DAYS` int(11) DEFAULT '0',
  `TOP_UP_ACKNOWLEDGED` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PLAYER_RELATIONSHIP` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `NICKNAME` varchar(255) DEFAULT NULL,
  `RELATIONSHIP_TYPE` int(11) DEFAULT NULL,
  `ESTABLISHED` datetime DEFAULT NULL,
  `COMMENT` varchar(255) DEFAULT NULL,
  `RELATED_PLAYER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PLAYER_ID`,`RELATED_PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PROGRESS_BAR` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `FACEBOOK_LIKE` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `ACCEPTED_INVITES` int(11) unsigned NOT NULL DEFAULT '0',
  `BONUS_ISSUED` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `BONUS_NOTIFIED` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `BONUS_CHIP_AMOUNT` int(11) unsigned NOT NULL DEFAULT '0',
  `REVIEWS` text,
  `FACEBOOK_PUBLISH_STREAM` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`PLAYER_ID`),
  CONSTRAINT `progress_bar_ibfk_1` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PROMOTION` (
  `PROMO_ID` int(11) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(30) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `TARGET_CLIENTS` varchar(128) NOT NULL DEFAULT '' COMMENT 'Comma-delimited list of clients: 1=Web, 2=iOS',
  `ALL_PLAYERS` tinyint(1) NOT NULL DEFAULT '0',
  `PLAYER_COUNT` int(11) NOT NULL DEFAULT '-1' COMMENT 'For promotion not marked for ''All Players'', a count of players added to promotion. -1 implies the count has not been calculated',
  `START_DATE` datetime NOT NULL,
  `END_DATE` datetime NOT NULL,
  `PRIORITY` int(11) DEFAULT NULL COMMENT 'determines which promotion to apply when several overlap',
  `SEED` int(11) NOT NULL DEFAULT '0',
  `CONTROL_GROUP_PERCENTAGE` int(11) NOT NULL DEFAULT '0',
  `CG_FUNCTION` varchar(50) DEFAULT 'PLAYER_ID',
  `is_player_list_inclusive` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`PROMO_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PROMOTION_CONFIG` (
  `PROMO_ID` int(11) NOT NULL,
  `CONFIG_KEY` varchar(50) NOT NULL,
  `CONFIG_VALUE` varchar(2000) NOT NULL,
  PRIMARY KEY (`PROMO_ID`,`CONFIG_KEY`),
  CONSTRAINT `promotion_config_ibfk_1` FOREIGN KEY (`PROMO_ID`) REFERENCES `PROMOTION` (`PROMO_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PROMOTION_PLAYER` (
  `PROMO_ID` int(11) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PROMO_ID`,`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PROMOTION_PLAYER_REWARD` (
  `PROMO_ID` int(11) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  `CONTROL_GROUP` tinyint(1) NOT NULL DEFAULT '0',
  `REWARDED_DATE` datetime NOT NULL,
  `DETAILS` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PROMO_ID`,`PLAYER_ID`,`REWARDED_DATE`),
  KEY `PLAYER_ID` (`PLAYER_ID`),
  KEY `REWARDED_DATE` (`REWARDED_DATE`),
  CONSTRAINT `promotion_player_reward_ibfk_1` FOREIGN KEY (`PROMO_ID`) REFERENCES `PROMOTION` (`PROMO_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `REQUESTMAP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `VERSION` bigint(20) NOT NULL DEFAULT '0',
  `CONFIG_ATTRIBUTE` varchar(255) NOT NULL,
  `URL` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UC_REQUESTMAP_URL` (`URL`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ROLE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `VERSION` bigint(20) NOT NULL DEFAULT '0',
  `AUTHORITY` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UC_ROLE_AUTHORITY` (`AUTHORITY`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `SYSTEM_CONFIG` (
  `CONFIG_KEY` varchar(255) NOT NULL,
  `CONFIG_VALUE` text,
  PRIMARY KEY (`CONFIG_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `SYSTEM_MESSAGE` (
  `SYSTEM_MESSAGE_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MESSAGE` text NOT NULL,
  `VALID_FROM` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `VALID_TO` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`SYSTEM_MESSAGE_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TABLE_INVITE` (
  `ID` bigint(20) NOT NULL,
  `TABLE_ID` bigint(20) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  `INVITE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `OPEN` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TABLE_STATUS` (
  `TABLE_STATUS` char(1) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`TABLE_STATUS`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TABLE_INFO` (
  `TABLE_ID` int(11) NOT NULL AUTO_INCREMENT,
  `GAME_VARIATION_TEMPLATE_ID` int(11) NOT NULL,
  `CURRENT_STATUS` longtext,
  `GAME_ID` int(11) DEFAULT NULL,
  `GAME_TYPE` varchar(255) NOT NULL,
  `TABLE_NAME` varchar(255) NOT NULL,
  `TABLE_ACCOUNT_ID` int(11) DEFAULT NULL,
  `PARTNER_ID` varchar(255) NOT NULL DEFAULT 'INTERNAL',
  `STATUS` char(1) NOT NULL DEFAULT 'C',
  `TSCREATED` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `CLIENT_ID` varchar(255) DEFAULT NULL,
  `SHOW_IN_LOBBY` bit(1) NOT NULL DEFAULT b'1',
  `OWNER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`TABLE_ID`),
  KEY `TABLE_INFO_ibfk_1` (`TABLE_ACCOUNT_ID`),
  KEY `TABLE_INFO_ibfk_2` (`GAME_VARIATION_TEMPLATE_ID`),
  KEY `TABLE_INFO_ibfk_3` (`GAME_TYPE`),
  KEY `TABLE_INFO_ibfk_4` (`STATUS`),
  KEY `TABLE_INFO_ibfk_5` (`PARTNER_ID`),
  KEY `TSCREATED` (`TSCREATED`),
  CONSTRAINT `TABLE_INFO_ibfk_1` FOREIGN KEY (`TABLE_ACCOUNT_ID`) REFERENCES `ACCOUNT` (`ACCOUNT_ID`),
  CONSTRAINT `TABLE_INFO_ibfk_2` FOREIGN KEY (`GAME_VARIATION_TEMPLATE_ID`) REFERENCES `GAME_VARIATION_TEMPLATE` (`GAME_VARIATION_TEMPLATE_ID`),
  CONSTRAINT `TABLE_INFO_ibfk_4` FOREIGN KEY (`STATUS`) REFERENCES `TABLE_STATUS` (`TABLE_STATUS`),
  CONSTRAINT `TABLE_INFO_ibfk_5` FOREIGN KEY (`PARTNER_ID`) REFERENCES `PARTNER` (`PARTNER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TABLE_GAME_VARIATION` (
  `TABLE_GAME_VARIATION_ID` int(11) NOT NULL AUTO_INCREMENT,
  `TABLE_ID` int(11) NOT NULL,
  `PROPERTY_NAME` varchar(255) NOT NULL,
  `PROPERTY_VALUE` text,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`TABLE_GAME_VARIATION_ID`),
  UNIQUE KEY `UC_TABLE_GAME_VARIATION_TABLE_ID_PROPERTY_NAME` (`TABLE_ID`,`PROPERTY_NAME`),
  CONSTRAINT `table_game_variation_ibfk_1` FOREIGN KEY (`TABLE_ID`) REFERENCES `TABLE_INFO` (`TABLE_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT` (
  `TOURNAMENT_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOURNAMENT_ACCOUNT_ID` int(11) DEFAULT NULL,
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL,
  `TOURNAMENT_CREATED_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `TOURNAMENT_SIGNUP_START_TS` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `TOURNAMENT_SIGNUP_END_TS` timestamp NULL DEFAULT NULL,
  `TOURNAMENT_START_TS` timestamp NULL DEFAULT NULL,
  `TOURNAMENT_STATUS` char(1) NOT NULL DEFAULT 'C',
  `TOURNAMENT_NAME` varchar(255) NOT NULL,
  `TOURNAMENT_POT_ACCOUNT_ID` int(11) DEFAULT NULL,
  `NEXT_EVENT_TS` timestamp NULL DEFAULT NULL,
  `PARTNER_ID` varchar(255) NOT NULL,
  `TOURNAMENT_DESCRIPTION` varchar(512) DEFAULT NULL,
  `TOURNAMENT_CURRENT_ROUND` int(11) DEFAULT NULL,
  `SETTLED_PRIZE_POT` decimal(64,4) DEFAULT NULL,
  `POT` decimal(64,4) NOT NULL DEFAULT '0.0000',
  PRIMARY KEY (`TOURNAMENT_ID`),
  KEY `FK_TOURNAMENT_ACCOUNT` (`TOURNAMENT_ACCOUNT_ID`),
  KEY `FK_TOURNAMENT_POT_ACCOUNT` (`TOURNAMENT_POT_ACCOUNT_ID`),
  CONSTRAINT `FK_TOURNAMENT_ACCOUNT` FOREIGN KEY (`TOURNAMENT_ACCOUNT_ID`) REFERENCES `ACCOUNT` (`ACCOUNT_ID`),
  CONSTRAINT `FK_TOURNAMENT_POT_ACCOUNT` FOREIGN KEY (`TOURNAMENT_POT_ACCOUNT_ID`) REFERENCES `ACCOUNT` (`ACCOUNT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_PLAYER` (
  `PLAYER_ID` bigint(20) NOT NULL,
  `TOURNAMENT_ACCOUNT_ID` int(11) NOT NULL,
  `TOURNAMENT_ID` bigint(20) NOT NULL,
  `PLAYER_STATUS` char(1) NOT NULL DEFAULT 'P',
  `LEADERBOARD_POSITION` smallint(6) NOT NULL DEFAULT '0',
  `NAME` varchar(255) NOT NULL DEFAULT 'unknown',
  `SETTLED_PRIZE` decimal(64,4) DEFAULT NULL,
  `ELIMINATION_TS` timestamp NULL DEFAULT NULL,
  `ELIMINATION_REASON` varchar(255) DEFAULT NULL,
  `PLAYER_PROPERTIES` text,
  PRIMARY KEY (`PLAYER_ID`,`TOURNAMENT_ACCOUNT_ID`,`TOURNAMENT_ID`),
  KEY `FK_TOURNAMENT_PLAYER_TOURNAMENT_ID` (`TOURNAMENT_ID`),
  KEY `FK_TOURNAMENT_PLAYER_ACCOUNT_ID` (`TOURNAMENT_ACCOUNT_ID`),
  CONSTRAINT `FK_TOURNAMENT_PLAYER_PLAYER_ID` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`),
  CONSTRAINT `FK_TOURNAMENT_PLAYER_TOURNAMENT_ID` FOREIGN KEY (`TOURNAMENT_ID`) REFERENCES `TOURNAMENT` (`TOURNAMENT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_SUMMARY` (
  `TOURNAMENT_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOURNAMENT_NAME` varchar(255) NOT NULL,
  `TOURNAMENT_FINISHED_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `TOURNAMENT_PLAYERS` text,
  PRIMARY KEY (`TOURNAMENT_ID`),
  KEY `IDX_TOURNAMENT_SUMMARY_TOURNAMENT_FINISHED_TS` (`TOURNAMENT_FINISHED_TS`),
  CONSTRAINT `FK_TOURNAMENT_SUM_TOURNAMENT` FOREIGN KEY (`TOURNAMENT_ID`) REFERENCES `TOURNAMENT` (`TOURNAMENT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_TABLE` (
  `TOURNAMENT_ID` bigint(20) NOT NULL,
  `TABLE_ID` int(11) NOT NULL,
  PRIMARY KEY (`TOURNAMENT_ID`,`TABLE_ID`),
  CONSTRAINT `FK_TOURNAMENT_TABLE_TOURNAMENT_ID` FOREIGN KEY (`TOURNAMENT_ID`) REFERENCES `TOURNAMENT` (`TOURNAMENT_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_VARIATION_TEMPLATE` (
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOURNAMENT_TYPE` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `ENTRY_FEE` decimal(64,4) DEFAULT NULL,
  `SERVICE_FEE` decimal(64,4) DEFAULT NULL,
  `STARTING_CHIPS` decimal(64,4) DEFAULT NULL,
  `MIN_PLAYERS` int(11) DEFAULT NULL,
  `MAX_PLAYERS` int(11) DEFAULT NULL,
  `GAME_TYPE` varchar(255) NOT NULL,
  `EXPIRY_DELAY` int(13) NOT NULL DEFAULT '86400000',
  `PRIZE_POOL` decimal(64,4) DEFAULT NULL,
  `ALLOCATOR` varchar(64) NOT NULL,
  PRIMARY KEY (`TOURNAMENT_VARIATION_TEMPLATE_ID`),
  KEY `FK_TOURNAMENT_VARIATION_TEMPLATE_GAME_TYPE` (`GAME_TYPE`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_VARIATION_ROUND` (
  `TOURNAMENT_VARIATION_ROUND_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL,
  `ROUND_NUMBER` int(11) NOT NULL,
  `ROUND_END_INTERVAL` int(13) NOT NULL,
  `ROUND_LENGTH` int(13) NOT NULL,
  `GAME_VARIATION_TEMPLATE_ID` int(11) NOT NULL,
  `CLIENT_PROPERTIES_ID` varchar(255) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `MINIMUM_BALANCE` decimal(64,4) NOT NULL,
  `DESCRIPTION` varchar(255) NOT NULL,
  PRIMARY KEY (`TOURNAMENT_VARIATION_ROUND_ID`),
  UNIQUE KEY `UC_TOURNAMENT_VARIATION_TEMPLATE_ROUND_NUMBER` (`TOURNAMENT_VARIATION_TEMPLATE_ID`,`ROUND_NUMBER`),
  KEY `FK_TOURNAMENT_ROUND_GAME_VARIATION_TEMPLATE_ID` (`GAME_VARIATION_TEMPLATE_ID`),
  CONSTRAINT `FK_TOURNAMENT_ROUND_GAME_VARIATION_TEMPLATE_ID` FOREIGN KEY (`GAME_VARIATION_TEMPLATE_ID`) REFERENCES `GAME_VARIATION_TEMPLATE` (`GAME_VARIATION_TEMPLATE_ID`),
  CONSTRAINT `FK_TOURNAMENT_ROUND_TOURNAMENT_VARIATION_TEMPLATE_ID` FOREIGN KEY (`TOURNAMENT_VARIATION_TEMPLATE_ID`) REFERENCES `TOURNAMENT_VARIATION_TEMPLATE` (`TOURNAMENT_VARIATION_TEMPLATE_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TOURNAMENT_VARIATION_PAYOUT` (
  `TOURNAMENT_VARIATION_PAYOUT_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL,
  `RANK` int(11) NOT NULL,
  `PAYOUT` decimal(9,8) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`TOURNAMENT_VARIATION_PAYOUT_ID`),
  UNIQUE KEY `UC_TOURNAMENT_VARIATION_TEMPLATE_RANK` (`TOURNAMENT_VARIATION_TEMPLATE_ID`,`RANK`),
  CONSTRAINT `FK_TOURNAMENT_PAYOUT_TOURNAMENT_VARIATION_TEMPLATE_ID` FOREIGN KEY (`TOURNAMENT_VARIATION_TEMPLATE_ID`) REFERENCES `TOURNAMENT_VARIATION_TEMPLATE` (`TOURNAMENT_VARIATION_TEMPLATE_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TRANSACTION_TYPE` (
  `TRANSACTION_TYPE` varchar(32) NOT NULL,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`TRANSACTION_TYPE`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `RECURRING_TOURNAMENT_DEFINITION` (
  `DEFINITION_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `INITIAL_SIGNUP_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `SIGNUP_PERIOD` bigint(20) NOT NULL,
  `FREQUENCY` bigint(20) NOT NULL,
  `TOURNAMENT_VARIATION_TEMPLATE_ID` bigint(20) NOT NULL,
  `TOURNAMENT_NAME` varchar(255) NOT NULL,
  `TOURNAMENT_DESCRIPTION` varchar(512) NOT NULL,
  `PARTNER_ID` varchar(255) NOT NULL,
  `EXCLUSION_PERIODS` text,
  `ENABLED` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`DEFINITION_ID`),
  KEY `FK_TOURNAMENT_VARIATION_TEMPLATE_ID` (`TOURNAMENT_VARIATION_TEMPLATE_ID`),
  CONSTRAINT `recurring_tournament_definition_ibfk_1` FOREIGN KEY (`TOURNAMENT_VARIATION_TEMPLATE_ID`) REFERENCES `TOURNAMENT_VARIATION_TEMPLATE` (`TOURNAMENT_VARIATION_TEMPLATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `TROPHY` (
  `TROPHY_ID` int(11) NOT NULL,
  `TROPHY_IMAGE` varchar(255) NOT NULL,
  `GAME_TYPE` varchar(255) NOT NULL,
  `TROPHY_NAME` varchar(255) NOT NULL,
  `MESSAGE` varchar(512) DEFAULT NULL,
  `SHORT_DESCRIPTION` varchar(255) DEFAULT NULL,
  `MESSAGE_CABINET` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`TROPHY_ID`),
  UNIQUE KEY `TROPHY_NAME` (`TROPHY_NAME`,`GAME_TYPE`),
  KEY `FK_TROPHY_GAME_TYPE` (`GAME_TYPE`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PLAYER_TROPHY` (
  `PLAYER_TROPHY_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PLAYER_ID` bigint(20) NOT NULL,
  `TROPHY_ID` int(11) NOT NULL,
  `DATE_AWARDED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PLAYER_TROPHY_ID`),
  UNIQUE KEY `uc_playerTrophy_playerTrophyDateAwarded` (`PLAYER_ID`,`TROPHY_ID`,`DATE_AWARDED`),
  KEY `TROPHY_ID` (`TROPHY_ID`),
  CONSTRAINT `player_trophy_ibfk_1` FOREIGN KEY (`PLAYER_ID`) REFERENCES `PLAYER` (`PLAYER_ID`),
  CONSTRAINT `player_trophy_ibfk_2` FOREIGN KEY (`TROPHY_ID`) REFERENCES `TROPHY` (`TROPHY_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `USER` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `VERSION` bigint(20) NOT NULL DEFAULT '0',
  `DESCRIPTION` varchar(255) NOT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `EMAIL_SHOW` bit(1) NOT NULL,
  `ENABLED` bit(1) NOT NULL,
  `PASSWD` varchar(255) NOT NULL,
  `USER_REAL_NAME` varchar(255) NOT NULL,
  `USERNAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UC_USER_USERNAME` (`USERNAME`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ROLE_PEOPLE` (
  `USER_ID` bigint(20) NOT NULL,
  `ROLE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ROLE_ID`,`USER_ID`),
  KEY `FK_ROLE_PEOPLE_USER` (`USER_ID`),
  CONSTRAINT `role_people_ibfk_1` FOREIGN KEY (`USER_ID`) REFERENCES `USER` (`ID`),
  CONSTRAINT `role_people_ibfk_2` FOREIGN KEY (`ROLE_ID`) REFERENCES `ROLE` (`ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `PARTNER_USERS` (
  `USER_ID` bigint(20) NOT NULL,
  `PARTNER_ID` varchar(255) NOT NULL,
  PRIMARY KEY (`USER_ID`,`PARTNER_ID`),
  KEY `FK_SYSTEM_PEOPLE_PARTNER` (`PARTNER_ID`),
  CONSTRAINT `partner_users_ibfk_1` FOREIGN KEY (`USER_ID`) REFERENCES `USER` (`ID`),
  CONSTRAINT `partner_users_ibfk_2` FOREIGN KEY (`PARTNER_ID`) REFERENCES `PARTNER` (`PARTNER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `YAZINO_LOGIN` (
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `PASSWORD_HASH` varchar(255) DEFAULT NULL,
  `PASSWORD_TYPE` varchar(10) NOT NULL DEFAULT 'MD5',
  `LOGIN_ATTEMPTS` smallint(5) unsigned NOT NULL DEFAULT '0',
  `SALT` binary(8) DEFAULT NULL,
  `LAST_MODIFIED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `PLAYER_ID` bigint(20) NOT NULL,
  UNIQUE KEY `UK_YAZINO_LOGIN_PLAYER_ID` (`PLAYER_ID`),
  UNIQUE KEY `UK_YAZINO_LOGIN_EMAIL_ADDRESS` (`EMAIL_ADDRESS`),
  CONSTRAINT `yazino_login_ibfk_2` FOREIGN KEY (`PLAYER_ID`) REFERENCES `LOBBY_USER` (`PLAYER_ID`)
) ENGINE=InnoDB DEFAULT charset=utf8#
-- /*!40101 SET character_set_client = @saved_cs_client */#
-- /*!40101 SET @saved_cs_client     = @@character_set_client */#
-- /*!40101 SET character_set_client = utf8 */#
CREATE TABLE `ZONG_TRANSACTION` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `STATUS` enum('REQUEST','SUCCESS','FAILURE') NOT NULL DEFAULT 'REQUEST',
  `USERID` int(11) NOT NULL,
  `NUMBEROFCHIPS` int(11) NOT NULL,
  `FAILURE` varchar(20) DEFAULT NULL,
  `GAMETYPE` varchar(20) NOT NULL,
  `TS_CREATED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `TS_CALLBACK` timestamp NULL DEFAULT NULL,
  `ITEMREF` varchar(20) DEFAULT NULL,
  `CONSUMERPRICE` decimal(16,4) DEFAULT NULL,
  `CONSUMERCURRENCY` varchar(4) DEFAULT NULL,
  `OUTPAYMENTAMOUNT` decimal(16,4) DEFAULT NULL,
  `OUTPAYMENTCURRENCY` varchar(4) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
-- /*!40101 SET character_set_client = @saved_cs_client */#
