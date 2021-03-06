CREATE TABLE IF NOT EXISTS `NOTIFICATION_CAMPAIGN` (
    `ID` int(11) NOT NULL auto_increment,
    `TITLE` varchar(256) DEFAULT '',
    `DESCRIPTION` varchar(256) DEFAULT '',
    `TRACKING` varchar(2048) DEFAULT NULL,
    `TARGET_COUNT` int(11) NOT NULL DEFAULT '0',
    `CREATED` datetime,
    `LAST_SENT` datetime,
    `RUN_PERIOD` varchar(256),
    `RUN_HOUR` INT(2),
    `RUN_MINUTE` INT(2),
    PRIMARY KEY (`ID`)
)#

DELETE FROM NOTIFICATION_CAMPAIGN#
INSERT INTO NOTIFICATION_CAMPAIGN VALUES (1, 'Progressive Bonus Reminder', 'Daily Progressive Bonus Reminder', 'FB_PlayedYesterday', 0, now(), null, 'DAILY', 5, 0)#

CREATE TABLE IF NOT EXISTS NOTIFICATION_TARGET (
  CAMPAIGN_ID int(11) NOT NULL,
  PLAYER_ID BIGINT(11) NOT NULL,
  PRIMARY KEY (CAMPAIGN_ID, PLAYER_ID),
  FOREIGN KEY (CAMPAIGN_ID) REFERENCES NOTIFICATION_CAMPAIGN(ID)
)#

CREATE TABLE IF NOT EXISTS CHANNEL_TYPE (
  ID int(11) NOT NULL,
  CHANNEL_NAME varchar(255),
  PRIMARY KEY (ID)
)#

DELETE FROM CHANNEL_TYPE#
INSERT INTO CHANNEL_TYPE VALUES (1, 'FACEBOOK_APP_TO_USER_REQUEST')#
INSERT INTO CHANNEL_TYPE VALUES (2, 'FACEBOOK_APP_TO_USER_NOTIFICATION')#
INSERT INTO CHANNEL_TYPE VALUES (3, 'IOS')#
INSERT INTO CHANNEL_TYPE VALUES (4, 'GOOGLE_CLOUD_MESSAGING_FOR_ANDROID')#

CREATE TABLE IF NOT EXISTS NOTIFICATION_CAMPAIGN_CHANNEL (
  CAMPAIGN_ID int(11),
  CHANNEL_ID int(11),
  PRIMARY KEY (CAMPAIGN_ID, CHANNEL_ID),
  FOREIGN KEY (CAMPAIGN_ID) REFERENCES NOTIFICATION_CAMPAIGN(ID),
  FOREIGN KEY (CHANNEL_ID) REFERENCES CHANNEL_TYPE(ID)
)#

