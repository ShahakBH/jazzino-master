DELETE FROM CAMPAIGN_CHANNEL_CONFIG WHERE CAMPAIGN_ID = -1337#
DELETE ss FROM SEGMENT_SELECTION ss LEFT JOIN CAMPAIGN_RUN cr ON ss.CAMPAIGN_RUN_ID=cr.ID AND cr.CAMPAIGN_ID = -1337#
DELETE FROM CAMPAIGN_RUN WHERE CAMPAIGN_ID = -1337#
DELETE FROM CAMPAIGN_CHANNEL WHERE CAMPAIGN_ID = -1337#
DELETE FROM CAMPAIGN_CONTENT WHERE CAMPAIGN_ID = -1337#
DELETE FROM CAMPAIGN_SCHEDULE WHERE CAMPAIGN_ID = -1337#
DELETE FROM CAMPAIGN_DEFINITION WHERE ID = -1337#