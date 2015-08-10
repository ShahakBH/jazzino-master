CREATE TABLE IF NOT EXISTS `MARKETING_GROUP` (
    `ID` int(11) auto_increment,
    `LABEL` varchar(255) NOT NULL,
    PRIMARY KEY (`ID`),
	CONSTRAINT `UC_MARKETING_GROUP_LABEL` UNIQUE (`LABEL`)
)#

CREATE TABLE IF NOT EXISTS `MARKETING_GROUP_MEMBER` (
    `MARKETING_GROUP_ID` int(11) NOT NULL,
    `PLAYER_ID` varchar(255) NOT NULL,
    `GAME_TYPE` varchar(255) NOT NULL,
    PRIMARY KEY (`MARKETING_GROUP_ID`, `PLAYER_ID`, `GAME_TYPE`),
    FOREIGN KEY (MARKETING_GROUP_ID) REFERENCES MARKETING_GROUP(ID) ON DELETE CASCADE
)#

DROP PROCEDURE if exists associate_marketing_group_with_app_request#

CREATE PROCEDURE associate_marketing_group_with_app_request(IN marketingGroupId INT, IN appRequestId INT)
BEGIN

    DECLARE channelType VARCHAR(255);
    SELECT channel_type INTO channelType FROM APP_REQUEST WHERE id = appRequestId;
           INSERT INTO strataproddw.APP_REQUEST_TARGET (app_request_id, player_id, external_id, game_type)
                SELECT DISTINCT appRequestId,
                       lu.player_id,
                       lu.external_id,
                       gm.game_type
                  FROM strataproddw.MARKETING_GROUP_MEMBER gm
                  JOIN strataprod.LOBBY_USER lu
                    ON (gm.player_id = lu.player_id)
       LEFT OUTER JOIN strataprod.IOS_PLAYER_DEVICE ios
                    ON (ios.player_id = lu.player_id AND ios.game_type = gm.game_type)
       LEFT OUTER JOIN strataproddw.GCM_PLAYER_DEVICE gcm
                    ON (gcm.player_id = lu.player_id AND gcm.game_type = gm.game_type)
                 WHERE gm.MARKETING_GROUP_ID = marketingGroupId
                   AND ((channelType = 'FACEBOOK_APP_TO_USER_REQUEST' AND provider_name = 'FACEBOOK')
                    OR (channelType = 'IOS' AND ios.device_token is not NULL)
                    OR (channelType = 'GOOGLE_CLOUD_MESSAGING_FOR_ANDROID' AND gcm.registration_id is not null));

    update APP_REQUEST set target_count = (select count(*) from APP_REQUEST_TARGET where app_request_id = appRequestId) where id = appRequestId;

end#

