DROP TABLE IF EXISTS rpt_promotion_uptake#
CREATE TABLE rpt_promotion_uptake (
    promo_id INT(11) NOT NULL,
    control_group TINYINT(1) NOT NULL,
    promotion_type VARCHAR(20),
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    unique_users_target INT(10) UNSIGNED,
    total_taken INT(10) UNSIGNED,
    unique_takers INT(10) UNSIGNED,
    chips_issued INT(10) UNSIGNED,
    total_revenue INT(11) UNSIGNED,
    PRIMARY KEY (promo_id, control_group) ) ENGINE=InnoDB#

DROP TABLE IF EXISTS rpt_promotion_uptake_daily#
CREATE TABLE rpt_promotion_uptake_daily (
    promo_id INT(11) NOT NULL,
    control_group TINYINT(1) NOT NULL,
    day_of_promotion INT(1) NOT NULL,
    total_taken INT(1) UNSIGNED NOT NULL,
    PRIMARY KEY (promo_id, control_group, day_of_promotion) ) ENGINE=InnoDB#

DROP TABLE IF EXISTS view_promotion_union#
DROP TABLE IF EXISTS view_promotion_config_union#
DROP TABLE IF EXISTS view_promotion_player_union#
DROP TABLE IF EXISTS view_promotion_player_reward_union#

CREATE TABLE view_promotion_union LIKE PROMOTION_ARCHIVE#
CREATE TABLE view_promotion_config_union LIKE PROMOTION_CONFIG_ARCHIVE#
CREATE TABLE view_promotion_player_union LIKE PROMOTION_PLAYER_ARCHIVE#
CREATE TABLE view_promotion_player_reward_union LIKE PROMOTION_PLAYER_REWARD_ARCHIVE#

ALTER TABLE view_promotion_player_reward_union
 ADD COLUMN revenue INT(11) UNSIGNED#

DROP FUNCTION IF EXISTS extractRevenueAttributableToPromotion#
CREATE FUNCTION extractRevenueAttributableToPromotion(DETAILS VARCHAR(255))
RETURNS INT
	DETERMINISTIC
		BEGIN
			DECLARE CHIPS_PART VARCHAR(255);
			DECLARE DEFAULT_CHIPS_PART VARCHAR(255);
			DECLARE ACTUAL_CHIPS_PART VARCHAR(255);
			DECLARE DEFAULT_CHIPS INT;
 			DECLARE ACTUAL_CHIPS INT;
			SET CHIPS_PART = SUBSTRING_INDEX(DETAILS, ',', -2);
			SET DEFAULT_CHIPS_PART := SUBSTRING_INDEX(CHIPS_PART, ',', 1);
			SET ACTUAL_CHIPS_PART := SUBSTRING_INDEX(CHIPS_PART, ',', -1);
			SET DEFAULT_CHIPS := CONVERT(TRIM(SUBSTRING_INDEX(DEFAULT_CHIPS_PART, '=', -1)), UNSIGNED);
 			SET ACTUAL_CHIPS := CONVERT(TRIM(SUBSTRING_INDEX(ACTUAL_CHIPS_PART, '=', -1)), UNSIGNED);

			RETURN IF(DEFAULT_CHIPS = ACTUAL_CHIPS, NULL, ACTUAL_CHIPS);
		END;
#

DROP FUNCTION IF EXISTS find_external_id_for_provider#
CREATE FUNCTION find_external_id_for_provider(PROVIDER_NAME VARCHAR(255),
                                              EXTERNAL_ID VARCHAR(255),
                                              LOBBY_USER_ID VARCHAR(255))  RETURNS VARCHAR(255)
DETERMINISTIC
RETURN (
    CASE WHEN UPPER(PROVIDER_NAME) = 'YAZINO' THEN
        LOBBY_USER_ID
    ELSE
        EXTERNAL_ID
    END
)#

-- isMemberOfControlGroup supersedes the control group functions in strataprod (deleted in strataprod delta 387)

DROP FUNCTION IF EXISTS isMemberOfControlGroup#
CREATE FUNCTION isMemberOfControlGroup(CG_FUNCTION VARCHAR(50),
                                       PROVIDER_NAME VARCHAR(255),
                                       EXTERNAL_ID VARCHAR(255),
                                       PLAYER_ID VARCHAR(255),
                                       LOBBY_USER_ID VARCHAR(255),
                                       SEED INT(11),
                                       CONTROL_GROUP_PERCENTAGE INT(11))
RETURNS TINYINT(1)
    DETERMINISTIC
		BEGIN

            DECLARE EXTERNAL_ID_FOR_PARTNER VARCHAR(255);

		    CASE CG_FUNCTION
        		WHEN 'PLAYER_ID' THEN
         			RETURN (((PLAYER_ID + SEED) % 100)  - CONTROL_GROUP_PERCENTAGE) < 0;
        		WHEN 'EXTERNAL_ID' THEN
            		SELECT find_external_id_for_provider(PROVIDER_NAME, EXTERNAL_ID, LOBBY_USER_ID) INTO EXTERNAL_ID_FOR_PARTNER;
            		RETURN (((CONV(SUBSTR(CONVERT(MD5(UPPER(CONCAT(PROVIDER_NAME, EXTERNAL_ID_FOR_PARTNER))), char), 25), 16, 10) + SEED) % 100) - CONTROL_GROUP_PERCENTAGE) < 0;
        		ELSE
            		BEGIN
                		SIGNAL SQLSTATE '45000'
                			SET MESSAGE_TEXT = "Unsupported control group function.";
            		END;
    		END CASE;
    	END;
#


--
-- This procedure is idempotent by design: it is more expensive to run but will not be affected by
-- duplicate or missed executions
--
DROP PROCEDURE IF EXISTS recreate_promotion_reports#
CREATE PROCEDURE recreate_promotion_reports()
BEGIN

    DELETE FROM rpt_promotion_uptake_daily;
    DELETE FROM rpt_promotion_uptake;

    DELETE FROM view_promotion_union;
    DELETE FROM view_promotion_config_union;
    DELETE FROM view_promotion_player_union;
    DELETE FROM view_promotion_player_reward_union;

                -- materialise promotion views
    INSERT INTO view_promotion_union
         SELECT *
           FROM PROMOTION_ARCHIVE
          WHERE ALL_PLAYERS = 0
          UNION (    SELECT * FROM strataprod.PROMOTION WHERE ALL_PLAYERS = 0   );

    INSERT INTO view_promotion_config_union
         SELECT *
           FROM PROMOTION_CONFIG_ARCHIVE
          UNION (    SELECT * FROM strataprod.PROMOTION_CONFIG    );

    INSERT INTO view_promotion_player_union
         SELECT *
           FROM PROMOTION_PLAYER_ARCHIVE
          UNION (    SELECT * FROM strataprod.PROMOTION_PLAYER    );

    INSERT INTO view_promotion_player_reward_union(PROMO_ID, PLAYER_ID, CONTROL_GROUP, REWARDED_DATE, DETAILS)
         SELECT *
           FROM PROMOTION_PLAYER_REWARD_ARCHIVE
          UNION (    SELECT * FROM strataprod.PROMOTION_PLAYER_REWARD    );

         UPDATE view_promotion_player_reward_union
     INNER JOIN view_promotion_union
          USING (PROMO_ID)
		    SET REVENUE = extractRevenueAttributableToPromotion(DETAILS)
	      WHERE TYPE = "BUY_CHIPS";

                -- insert 2 rows for each promotion: 1 for the experimental group and 1 for the control group
    INSERT INTO rpt_promotion_uptake (promo_id, control_group, promotion_type, start_date, end_date)
         SELECT PROMO_ID, CONTROL_GROUP, P.TYPE, START_DATE, END_DATE
           FROM view_promotion_union AS P
           JOIN (SELECT 0 AS CONTROL_GROUP UNION ALL SELECT 1) AS G;

                -- populate unique users target
         UPDATE rpt_promotion_uptake AS REPORT
      LEFT JOIN (   SELECT PROMO_ID,
                           isMemberOfControlGroup(CG_FUNCTION, PROVIDER_NAME, PLAYER.EXTERNAL_ID, PLAYER_ID, PLAYER.USER_ID, SEED, CONTROL_GROUP_PERCENTAGE) AS CONTROL_GROUP,
                           PLAYER_ID,
                           COUNT(1) AS TOTAL
                      FROM view_promotion_union
                INNER JOIN view_promotion_player_union
                     USING (PROMO_ID)
                INNER JOIN strataprod.PLAYER
                     USING (PLAYER_ID)
                INNER JOIN strataprod.LOBBY_USER
                     USING (PLAYER_ID)
                  GROUP BY PROMO_ID, CONTROL_GROUP   ) AS SOURCE
          USING (PROMO_ID, CONTROL_GROUP)
            SET REPORT.UNIQUE_USERS_TARGET = IFNULL(SOURCE.TOTAL, 0);

             -- populate unique takers, total taken
          UPDATE rpt_promotion_uptake AS REPORT
       LEFT JOIN (    SELECT PROMO_ID, CONTROL_GROUP, COUNT(DISTINCT PLAYER_ID) AS DISTINCT_TOTAL, COUNT(*) AS TOTAL_TAKEN
                        FROM view_promotion_player_reward_union
                    GROUP BY PROMO_ID, CONTROL_GROUP    ) AS SOURCE
           USING (PROMO_ID, CONTROL_GROUP)
             SET REPORT.UNIQUE_TAKERS = IFNULL(SOURCE.DISTINCT_TOTAL, 0),
                 REPORT.TOTAL_TAKEN = IFNULL(SOURCE.TOTAL_TAKEN, 0);

                 -- populate chips issued
          UPDATE rpt_promotion_uptake AS REPORT
      INNER JOIN (    SELECT PROMO_ID, CONFIG_KEY, CONVERT(CONFIG_VALUE, SIGNED) AS REWARD
                        FROM view_promotion_config_union
                       WHERE CONFIG_KEY = 'reward.chips'    ) AS CONFIG
           USING (PROMO_ID)
       LEFT JOIN (    SELECT PROMO_ID, CONTROL_GROUP, COUNT(DISTINCT PLAYER_ID) AS DISTINCT_TOTAL, COUNT(*) AS TOTAL_TAKEN
                        FROM view_promotion_player_reward_union
                    GROUP BY PROMO_ID, CONTROL_GROUP    ) AS SOURCE
           USING (PROMO_ID, CONTROL_GROUP)
             SET REPORT.UNIQUE_TAKERS = IFNULL(SOURCE.DISTINCT_TOTAL, 0),
                 REPORT.TOTAL_TAKEN = IFNULL(SOURCE.TOTAL_TAKEN, 0),
                 REPORT.CHIPS_ISSUED = IFNULL(SOURCE.TOTAL_TAKEN, 0) * IFNULL(CONFIG.REWARD, 0)
           WHERE PROMOTION_TYPE = 'DAILY_AWARD';

                  -- populate total revenue
           UPDATE rpt_promotion_uptake AS REPORT
        LEFT JOIN (    SELECT PROMO_ID, CONTROL_GROUP, SUM(REVENUE) AS TOTAL_REVENUE
                         FROM view_promotion_player_reward_union
                     GROUP BY PROMO_ID, CONTROL_GROUP    ) AS SOURCE
            USING (PROMO_ID, CONTROL_GROUP)
              SET REPORT.TOTAL_REVENUE = IFNULL(SOURCE.TOTAL_REVENUE, 0)
            WHERE PROMOTION_TYPE = 'BUY_CHIPS';
                  -- TODO unique takers, total taken

     INSERT INTO rpt_promotion_uptake_daily (PROMO_ID, CONTROL_GROUP, DAY_OF_PROMOTION, TOTAL_TAKEN)
          SELECT PROMO_ID,
                 CONTROL_GROUP,
                 1 + FLOOR(((TO_SECONDS(REWARDED_DATE) - TO_SECONDS(START_DATE)) * 1.0)/(60*60*24.0)) AS DAY_OF_PROMOTION,
                 COUNT(DISTINCT PLAYER_ID) AS TOTAL_TAKEN
            FROM view_promotion_player_reward_union
      INNER JOIN view_promotion_union
           USING (PROMO_ID)
        GROUP BY PROMO_ID, CONTROL_GROUP, DAY_OF_PROMOTION
          HAVING DAY_OF_PROMOTION <= 7;
END#
