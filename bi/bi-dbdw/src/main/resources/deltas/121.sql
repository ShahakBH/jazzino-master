DROP VIEW IF EXISTS bi_verification_status_per_10_mins#

DROP VIEW IF EXISTS bi_verification_status_per_hour#
CREATE VIEW bi_verification_status_per_hour AS
    SELECT
		CONCAT(DAY(rr.AUDIT_TIME),'/',MONTH(rr.AUDIT_TIME),'/',YEAR(rr.AUDIT_TIME),' ',HOUR(rr.AUDIT_TIME),':00') AS TIME_PERIOD,
		rr.RPX as RPX,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0),
					0
				),
				0
			)
		) AS  PLAYED_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1),
					0
				),
				0
			)
		) AS PLAYED_NOT_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0)
				),
				0
			)
		) AS  NOT_PLAYED_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1)
				),
				0
			)
		) AS  NOT_PLAYED_NOT_VERIFIED

		FROM rpt_recent_registrations rr
		JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
		JOIN LOBBY_USER lu USING (USER_ID)
		WHERE rr.RPX != 'google'
		GROUP BY CONCAT(DAY(rr.AUDIT_TIME),MONTH(rr.AUDIT_TIME),YEAR(rr.AUDIT_TIME),HOUR(rr.AUDIT_TIME)), rr.RPX
		ORDER BY rr.AUDIT_TIME DESC#


DROP VIEW IF EXISTS bi_verification_status_per_day#
CREATE VIEW bi_verification_status_per_day AS
	SELECT
		CONCAT(DAY(rr.AUDIT_TIME),'/',MONTH(rr.AUDIT_TIME),'/',YEAR(rr.AUDIT_TIME)) AS TIME_PERIOD,
		rr.RPX as RPX,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0),
					0
				),
				0
			)
		) AS  PLAYED_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1),
					0
				),
				0
			)
		) AS PLAYED_NOT_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0)
				),
				0
			)
		) AS  NOT_PLAYED_VERIFIED,
		SUM(
			IF(rr.RPX=('YAZINO' OR 'facebook' OR 'FACEBOOK'),
				IF(rr.PLAYED>0,
					0,
					IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1)
				),
				0
			)
		) AS  NOT_PLAYED_NOT_VERIFIED

		FROM rpt_recent_registrations rr
		JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
		JOIN LOBBY_USER lu USING (USER_ID)
		WHERE rr.RPX != 'google'
		GROUP BY CONCAT(DAY(rr.AUDIT_TIME),MONTH(rr.AUDIT_TIME),YEAR(rr.AUDIT_TIME)), rr.RPX
		ORDER BY rr.AUDIT_TIME DESC#

