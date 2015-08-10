DROP TABLE IF EXISTS `TRACKING_EVENT_PROPERTY`#
DROP TABLE IF EXISTS `TRACKING_EVENT`#

CREATE TABLE IF NOT EXISTS `TRACKING_EVENT` (
    `ID` bigint(20) auto_increment,
    `PLATFORM` varchar(20),
    `PLAYER_ID` bigint(20) NOT NULL,
    `NAME` varchar(255) NOT NULL,
    `RECEIVED` timestamp NOT NULL,
	PRIMARY KEY (`ID`),
	KEY `IDX_TRACKING_EVENT_RECEIVED` (`RECEIVED`)
)#

CREATE TABLE IF NOT EXISTS `TRACKING_EVENT_PROPERTY` (
    `ID` bigint(20) auto_increment,
    `TRACKING_EVENT_ID` bigint(20),
    `KEY` varchar(20) NOT NULL,
    `VALUE` varchar(255) NOT NULL,
    `RECEIVED` timestamp NOT NULL, -- denormalized to for partition management (properties must be dropped together with their events)
	PRIMARY KEY (`ID`),
	KEY `IDX_TRACKING_EVENT_PROPERTY_TRACKING_EVENT_ID` (`TRACKING_EVENT_ID`),
	KEY `IDX_TRACKING_EVENT_PROPERTY_RECEIVED` (`RECEIVED`)
)#

