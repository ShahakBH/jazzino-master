-- WEB-3860 - Fix TEXAS_HOLDEM metadata to indicate tournament support

INSERT INTO `GAME_CONFIGURATION_PROPERTY` (GAME_ID, PROPERTY_NAME, PROPERTY_VALUE) VALUES ('TEXAS_HOLDEM', 'supportsTournaments', 'true')#
