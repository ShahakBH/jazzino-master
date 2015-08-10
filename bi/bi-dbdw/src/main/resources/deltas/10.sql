rename table TRANSACTION_LOG to TRANSACTION_LOG_OLD#

CREATE TABLE TRANSACTION_LOG (
  `TRANSACTION_LOG_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ACCOUNT_ID` int(11) NOT NULL,
  `AMOUNT` decimal(64,4) NOT NULL,
  `REFERENCE` varchar(255) DEFAULT NULL,
  `TSEXECUTED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `TRANSACTION_TYPE` varchar(32) NOT NULL,
  `AUDIT_LABEL` varchar(255) DEFAULT NULL,
  `TRANSACTION_TS` timestamp NULL DEFAULT NULL,
  `RUNNING_BALANCE` decimal(64,4) DEFAULT NULL,
  PRIMARY KEY (`TRANSACTION_LOG_ID`, TRANSACTION_TS),
  KEY `IDX_REFERENCE` (`REFERENCE`),
  KEY `IDX_ACCOUNT_ID` (`ACCOUNT_ID`),
  KEY `IDX_TRANSACTION_LOG_TRANSACTION_TYPE` (`TRANSACTION_TYPE`),
  KEY `IDX_TRANSACTION_LOG_REF_TX_TYPE` (`REFERENCE`,`TRANSACTION_TYPE`),
  KEY `IDX_AUDIT_LABEL` (`AUDIT_LABEL`),
  KEY `IDX_TRANSACTION_TS` (`TRANSACTION_TS`)
)#

-- PARTITION BY RANGE (UNIX_TIMESTAMP(TRANSACTION_TS)) (
-- 	PARTITION p20110325 values less than (UNIX_TIMESTAMP('2011-03-26 00:00:00')),
-- 	PARTITION p20110326 values less than (UNIX_TIMESTAMP('2011-03-27 00:00:00')),
-- 	PARTITION p20110327 values less than (UNIX_TIMESTAMP('2011-03-28 00:00:00')),
-- 	PARTITION p20110328 values less than (UNIX_TIMESTAMP('2011-03-29 00:00:00')),
-- 	PARTITION p20110329 values less than (UNIX_TIMESTAMP('2011-03-30 00:00:00')),
-- 	PARTITION p20110330 values less than (UNIX_TIMESTAMP('2011-03-31 00:00:00')),
-- 	PARTITION p20110331 values less than (UNIX_TIMESTAMP('2011-04-01 00:00:00')),
-- 	PARTITION p20110401 values less than (UNIX_TIMESTAMP('2011-04-02 00:00:00')),
-- 	PARTITION p20110402 values less than (UNIX_TIMESTAMP('2011-04-03 00:00:00')),
-- 	PARTITION p20110403 values less than (UNIX_TIMESTAMP('2011-04-04 00:00:00')),
-- 	PARTITION p20110404 values less than (UNIX_TIMESTAMP('2011-04-05 00:00:00')),
-- 	PARTITION p20110405 values less than (UNIX_TIMESTAMP('2011-04-06 00:00:00')),
-- 	PARTITION p20110406 values less than (UNIX_TIMESTAMP('2011-04-07 00:00:00')),
-- 	PARTITION p20110407 values less than (UNIX_TIMESTAMP('2011-04-08 00:00:00')),
-- 	PARTITION p20110408 values less than (UNIX_TIMESTAMP('2011-04-09 00:00:00')),
-- 	PARTITION p20110409 values less than (UNIX_TIMESTAMP('2011-04-10 00:00:00')),
-- 	PARTITION p20110410 values less than (UNIX_TIMESTAMP('2011-04-11 00:00:00')),
-- 	PARTITION p20110411 values less than (UNIX_TIMESTAMP('2011-04-12 00:00:00')),
-- 	PARTITION p20110412 values less than (UNIX_TIMESTAMP('2011-04-13 00:00:00')),
-- 	PARTITION p20110413 values less than (UNIX_TIMESTAMP('2011-04-14 00:00:00')),
-- 	PARTITION p20110414 values less than (UNIX_TIMESTAMP('2011-04-15 00:00:00')),
-- 	PARTITION p20110415 values less than (UNIX_TIMESTAMP('2011-04-16 00:00:00')),
-- 	PARTITION p20110416 values less than (UNIX_TIMESTAMP('2011-04-17 00:00:00')),
-- 	PARTITION p20110417 values less than (UNIX_TIMESTAMP('2011-04-18 00:00:00')),
-- 	PARTITION p20110418 values less than (UNIX_TIMESTAMP('2011-04-19 00:00:00')),
-- 	PARTITION p20110419 values less than (UNIX_TIMESTAMP('2011-04-20 00:00:00')),
-- 	PARTITION p20110420 values less than (UNIX_TIMESTAMP('2011-04-21 00:00:00')),
-- 	PARTITION p20110421 values less than (UNIX_TIMESTAMP('2011-04-22 00:00:00')),
-- 	PARTITION p20110422 values less than (UNIX_TIMESTAMP('2011-04-23 00:00:00')),
-- 	PARTITION pDefault values less than (MAXVALUE)
-- )#

insert into TRANSACTION_LOG select * from TRANSACTION_LOG_OLD#

rename table AUDIT_CLOSED_GAME to AUDIT_CLOSED_GAME_OLD#

CREATE TABLE `AUDIT_CLOSED_GAME` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `AUDIT_LABEL` varchar(255) NOT NULL,
  `HOSTNAME` varchar(255) NOT NULL,
  `AUDIT_TS` datetime NOT NULL,
  `DBWRITE_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `TABLE_ID` decimal(64,2) DEFAULT NULL,
  `GAME_ID` int(11) DEFAULT NULL,
  `GAME_INCREMENT` int(11) DEFAULT NULL,
  `OBSERVABLE_STATUS` longtext,
  `INTERNAL_STATUS` longtext,
  PRIMARY KEY (`auto_id`, AUDIT_TS),
  KEY `IDX_TABLE_ID` (`TABLE_ID`),
  KEY `IDX_GAME_ID` (`GAME_ID`),
  KEY `IDX_AUDIT_CLOSED_GAME_AUDIT_TS` (`AUDIT_TS`),
  KEY `IDX_AUDIT_CLOSED_GAME_HOSTNAME` (`HOSTNAME`)
)#
--   PARTITION BY RANGE (to_days(AUDIT_TS)) (
-- 	PARTITION p20110325 values less than (to_days('2011-03-26')),
-- 	PARTITION p20110326 values less than (to_days('2011-03-27')),
-- 	PARTITION p20110327 values less than (to_days('2011-03-28')),
-- 	PARTITION p20110328 values less than (to_days('2011-03-29')),
-- 	PARTITION p20110329 values less than (to_days('2011-03-30')),
-- 	PARTITION p20110330 values less than (to_days('2011-03-31')),
-- 	PARTITION p20110331 values less than (to_days('2011-04-01')),
-- 	PARTITION p20110401 values less than (to_days('2011-04-02')),
-- 	PARTITION p20110402 values less than (to_days('2011-04-03')),
-- 	PARTITION p20110403 values less than (to_days('2011-04-04')),
-- 	PARTITION p20110404 values less than (to_days('2011-04-05')),
-- 	PARTITION p20110405 values less than (to_days('2011-04-06')),
-- 	PARTITION p20110406 values less than (to_days('2011-04-07')),
-- 	PARTITION p20110407 values less than (to_days('2011-04-08')),
-- 	PARTITION p20110408 values less than (to_days('2011-04-09')),
-- 	PARTITION p20110409 values less than (to_days('2011-04-10')),
-- 	PARTITION p20110410 values less than (to_days('2011-04-11')),
-- 	PARTITION p20110411 values less than (to_days('2011-04-12')),
-- 	PARTITION p20110412 values less than (to_days('2011-04-13')),
-- 	PARTITION p20110413 values less than (to_days('2011-04-14')),
-- 	PARTITION p20110414 values less than (to_days('2011-04-15')),
-- 	PARTITION p20110415 values less than (to_days('2011-04-16')),
-- 	PARTITION p20110416 values less than (to_days('2011-04-17')),
-- 	PARTITION p20110417 values less than (to_days('2011-04-18')),
-- 	PARTITION p20110418 values less than (to_days('2011-04-19')),
-- 	PARTITION p20110419 values less than (to_days('2011-04-20')),
-- 	PARTITION p20110420 values less than (to_days('2011-04-21')),
-- 	PARTITION p20110421 values less than (to_days('2011-04-22')),
-- 	PARTITION p20110422 values less than (to_days('2011-04-23')),
-- 	PARTITION pDefault values less than (MAXVALUE)
-- )#

insert into AUDIT_CLOSED_GAME select * from AUDIT_CLOSED_GAME_OLD#

rename table AUDIT_CLOSED_GAME_PLAYER to AUDIT_CLOSED_GAME_PLAYER_OLD#

CREATE TABLE `AUDIT_CLOSED_GAME_PLAYER` (
  `auto_id` int(11) NOT NULL AUTO_INCREMENT,
  `AUDIT_LABEL` varchar(255) NOT NULL,
  `HOSTNAME` varchar(255) NOT NULL,
  `AUDIT_TS` datetime NOT NULL,
  `DBWRITE_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `TABLE_ID` decimal(64,2) NOT NULL,
  `GAME_ID` decimal(64,2) NOT NULL,
  `PLAYER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`auto_id`, AUDIT_TS),
  KEY `AUDIT_LABEL` (`AUDIT_LABEL`),
  KEY `IDX_ACGP_P` (`PLAYER_ID`,`GAME_ID`),
  KEY `IDX_ACGP_P1` (`PLAYER_ID`)
)#
--   PARTITION BY RANGE (to_days(AUDIT_TS)) (
-- 	PARTITION p20110325 values less than (to_days('2011-03-26')),
-- 	PARTITION p20110326 values less than (to_days('2011-03-27')),
-- 	PARTITION p20110327 values less than (to_days('2011-03-28')),
-- 	PARTITION p20110328 values less than (to_days('2011-03-29')),
-- 	PARTITION p20110329 values less than (to_days('2011-03-30')),
-- 	PARTITION p20110330 values less than (to_days('2011-03-31')),
-- 	PARTITION p20110331 values less than (to_days('2011-04-01')),
-- 	PARTITION p20110401 values less than (to_days('2011-04-02')),
-- 	PARTITION p20110402 values less than (to_days('2011-04-03')),
-- 	PARTITION p20110403 values less than (to_days('2011-04-04')),
-- 	PARTITION p20110404 values less than (to_days('2011-04-05')),
-- 	PARTITION p20110405 values less than (to_days('2011-04-06')),
-- 	PARTITION p20110406 values less than (to_days('2011-04-07')),
-- 	PARTITION p20110407 values less than (to_days('2011-04-08')),
-- 	PARTITION p20110408 values less than (to_days('2011-04-09')),
-- 	PARTITION p20110409 values less than (to_days('2011-04-10')),
-- 	PARTITION p20110410 values less than (to_days('2011-04-11')),
-- 	PARTITION p20110411 values less than (to_days('2011-04-12')),
-- 	PARTITION p20110412 values less than (to_days('2011-04-13')),
-- 	PARTITION p20110413 values less than (to_days('2011-04-14')),
-- 	PARTITION p20110414 values less than (to_days('2011-04-15')),
-- 	PARTITION p20110415 values less than (to_days('2011-04-16')),
-- 	PARTITION p20110416 values less than (to_days('2011-04-17')),
-- 	PARTITION p20110417 values less than (to_days('2011-04-18')),
-- 	PARTITION p20110418 values less than (to_days('2011-04-19')),
-- 	PARTITION p20110419 values less than (to_days('2011-04-20')),
-- 	PARTITION p20110420 values less than (to_days('2011-04-21')),
-- 	PARTITION p20110421 values less than (to_days('2011-04-22')),
-- 	PARTITION p20110422 values less than (to_days('2011-04-23')),
-- 	PARTITION pDefault values less than (MAXVALUE)
-- )#

insert into AUDIT_CLOSED_GAME_PLAYER select * from AUDIT_CLOSED_GAME_PLAYER_OLD#

rename table AUDIT_COMMAND to AUDIT_COMMAND_OLD#

CREATE TABLE `AUDIT_COMMAND` (
  `auto_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `AUDIT_LABEL` varchar(255) NOT NULL,
  `HOSTNAME` varchar(255) NOT NULL,
  `AUDIT_TS` datetime NOT NULL,
  `DBWRITE_TS` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `COMMAND_ID` varchar(255) NOT NULL,
  `TABLE_ID` decimal(64,2) NOT NULL,
  `GAME_ID` bigint(20) DEFAULT NULL,
  `ACCOUNT_ID` decimal(64,2) NOT NULL,
  `COMMAND_TYPE` varchar(255) NOT NULL,
  `COMMAND_ARGS` text,
  PRIMARY KEY (`auto_id`, AUDIT_TS),
  KEY `AUDIT_LABEL` (`AUDIT_LABEL`),
  KEY `IDX_AUDIT_COMMAND_AUDIT_TS` (`AUDIT_TS`)
)#
--   PARTITION BY RANGE (to_days(AUDIT_TS)) (
-- 	PARTITION p20110325 values less than (to_days('2011-03-26')),
-- 	PARTITION p20110326 values less than (to_days('2011-03-27')),
-- 	PARTITION p20110327 values less than (to_days('2011-03-28')),
-- 	PARTITION p20110328 values less than (to_days('2011-03-29')),
-- 	PARTITION p20110329 values less than (to_days('2011-03-30')),
-- 	PARTITION p20110330 values less than (to_days('2011-03-31')),
-- 	PARTITION p20110331 values less than (to_days('2011-04-01')),
-- 	PARTITION p20110401 values less than (to_days('2011-04-02')),
-- 	PARTITION p20110402 values less than (to_days('2011-04-03')),
-- 	PARTITION p20110403 values less than (to_days('2011-04-04')),
-- 	PARTITION p20110404 values less than (to_days('2011-04-05')),
-- 	PARTITION p20110405 values less than (to_days('2011-04-06')),
-- 	PARTITION p20110406 values less than (to_days('2011-04-07')),
-- 	PARTITION p20110407 values less than (to_days('2011-04-08')),
-- 	PARTITION p20110408 values less than (to_days('2011-04-09')),
-- 	PARTITION p20110409 values less than (to_days('2011-04-10')),
-- 	PARTITION p20110410 values less than (to_days('2011-04-11')),
-- 	PARTITION p20110411 values less than (to_days('2011-04-12')),
-- 	PARTITION p20110412 values less than (to_days('2011-04-13')),
-- 	PARTITION p20110413 values less than (to_days('2011-04-14')),
-- 	PARTITION p20110414 values less than (to_days('2011-04-15')),
-- 	PARTITION p20110415 values less than (to_days('2011-04-16')),
-- 	PARTITION p20110416 values less than (to_days('2011-04-17')),
-- 	PARTITION p20110417 values less than (to_days('2011-04-18')),
-- 	PARTITION p20110418 values less than (to_days('2011-04-19')),
-- 	PARTITION p20110419 values less than (to_days('2011-04-20')),
-- 	PARTITION p20110420 values less than (to_days('2011-04-21')),
-- 	PARTITION p20110421 values less than (to_days('2011-04-22')),
-- 	PARTITION p20110422 values less than (to_days('2011-04-23')),
-- 	PARTITION pDefault values less than (MAXVALUE)
-- )#

insert into AUDIT_COMMAND select * from AUDIT_COMMAND#

rename table ACCOUNT_SESSION to ACCOUNT_SESSION_OLD#

CREATE TABLE `ACCOUNT_SESSION` (
  `SESSION_ID` int(11) NOT NULL AUTO_INCREMENT,
  `ACCOUNT_ID` int(11) NOT NULL,
  `SESSION_KEY` varchar(255) NOT NULL,
  `TSSTARTED` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `VERSION` int(11) NOT NULL DEFAULT '0',
  `IP_ADDRESS` varchar(40) DEFAULT NULL,
  `REFERER` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`SESSION_ID`, TSSTARTED),
  KEY `IDX_SESSION_ACCOUNT` (`ACCOUNT_ID`),
  KEY `IDX_ACCOUNT_SESSION_TSSTARTED` (`TSSTARTED`)
)#
--   PARTITION BY RANGE (UNIX_TIMESTAMP(TSSTARTED)) (
-- 	PARTITION p20110325 values less than (UNIX_TIMESTAMP('2011-03-26 00:00:00')),
-- 	PARTITION p20110326 values less than (UNIX_TIMESTAMP('2011-03-27 00:00:00')),
-- 	PARTITION p20110327 values less than (UNIX_TIMESTAMP('2011-03-28 00:00:00')),
-- 	PARTITION p20110328 values less than (UNIX_TIMESTAMP('2011-03-29 00:00:00')),
-- 	PARTITION p20110329 values less than (UNIX_TIMESTAMP('2011-03-30 00:00:00')),
-- 	PARTITION p20110330 values less than (UNIX_TIMESTAMP('2011-03-31 00:00:00')),
-- 	PARTITION p20110331 values less than (UNIX_TIMESTAMP('2011-04-01 00:00:00')),
-- 	PARTITION p20110401 values less than (UNIX_TIMESTAMP('2011-04-02 00:00:00')),
-- 	PARTITION p20110402 values less than (UNIX_TIMESTAMP('2011-04-03 00:00:00')),
-- 	PARTITION p20110403 values less than (UNIX_TIMESTAMP('2011-04-04 00:00:00')),
-- 	PARTITION p20110404 values less than (UNIX_TIMESTAMP('2011-04-05 00:00:00')),
-- 	PARTITION p20110405 values less than (UNIX_TIMESTAMP('2011-04-06 00:00:00')),
-- 	PARTITION p20110406 values less than (UNIX_TIMESTAMP('2011-04-07 00:00:00')),
-- 	PARTITION p20110407 values less than (UNIX_TIMESTAMP('2011-04-08 00:00:00')),
-- 	PARTITION p20110408 values less than (UNIX_TIMESTAMP('2011-04-09 00:00:00')),
-- 	PARTITION p20110409 values less than (UNIX_TIMESTAMP('2011-04-10 00:00:00')),
-- 	PARTITION p20110410 values less than (UNIX_TIMESTAMP('2011-04-11 00:00:00')),
-- 	PARTITION p20110411 values less than (UNIX_TIMESTAMP('2011-04-12 00:00:00')),
-- 	PARTITION p20110412 values less than (UNIX_TIMESTAMP('2011-04-13 00:00:00')),
-- 	PARTITION p20110413 values less than (UNIX_TIMESTAMP('2011-04-14 00:00:00')),
-- 	PARTITION p20110414 values less than (UNIX_TIMESTAMP('2011-04-15 00:00:00')),
-- 	PARTITION p20110415 values less than (UNIX_TIMESTAMP('2011-04-16 00:00:00')),
-- 	PARTITION p20110416 values less than (UNIX_TIMESTAMP('2011-04-17 00:00:00')),
-- 	PARTITION p20110417 values less than (UNIX_TIMESTAMP('2011-04-18 00:00:00')),
-- 	PARTITION p20110418 values less than (UNIX_TIMESTAMP('2011-04-19 00:00:00')),
-- 	PARTITION p20110419 values less than (UNIX_TIMESTAMP('2011-04-20 00:00:00')),
-- 	PARTITION p20110420 values less than (UNIX_TIMESTAMP('2011-04-21 00:00:00')),
-- 	PARTITION p20110421 values less than (UNIX_TIMESTAMP('2011-04-22 00:00:00')),
-- 	PARTITION p20110422 values less than (UNIX_TIMESTAMP('2011-04-23 00:00:00')),
-- 	PARTITION pDefault values less than (MAXVALUE)
-- )#
insert into ACCOUNT_SESSION select * from ACCOUNT_SESSION_OLD#
