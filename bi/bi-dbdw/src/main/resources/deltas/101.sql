ALTER TABLE `IOS_PUSH_NOTIFICATION` CHANGE `MESSAGES_SENT_COUNT` `TARGET_COUNT` int(11) NOT NULL DEFAULT '0'#

ALTER TABLE `IOS_PUSH_NOTIFICATION_TARGET` ADD UNIQUE INDEX IDX_TARGET (NOTIFICATION_ID, GAME_TYPE, PLAYER_ID)#