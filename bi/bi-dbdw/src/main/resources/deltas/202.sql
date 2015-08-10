-- for first day
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 1', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(now(), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Collect your FREE DAILY BONUS! Race to a game now.')#

-- For day + 1
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 2', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 1 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Earn FREE BONUS CHIPS every day you play at Yazino. Join a game!')#

-- For day + 2
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 3', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 2 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Get even more FREE BONUS CHIPS. Play now to collect them!')#


-- For day + 3
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 4', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 3 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Your FREE DAILY BONUS is waiting for you! Play now.')#

-- For day + 4
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 5', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 4 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Get even more FREE BONUS CHIPS when you play now!')#

-- For day + 5
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 6', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 5 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Increase your DAILY BONUS CHIPS every day you play. Fire up a game now!')#

-- For day + 6
INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery) VALUES ('Progressive Daily Reminder 7', 'SELECT player_id FROM PLAYER_ACTIVITY_DAILY WHERE activity_ts >= CURRENT_DATE - INTERVAL \'1 day\' and activity_ts < CURRENT_DATE')#
INSERT INTO CAMPAIGN_SCHEDULE VALUES (LAST_INSERT_ID(), DATE_FORMAT(DATE_ADD(now(), INTERVAL 6 DAY), '%Y-%m-%d 05:00:00'))#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'description', 'message description')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'tracking', 'FB_PlayedYesterday')#
INSERT INTO CAMPAIGN_CONTENT VALUES (LAST_INSERT_ID(), 'message', 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Grab your chips and win big now!')#





