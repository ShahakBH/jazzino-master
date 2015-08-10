DROP PROCEDURE IF EXISTS alterDailyMailStats
#
CREATE PROCEDURE alterDailyMailStats()
BEGIN
	declare marker bigint(21) default 0;
	
	-- Table info schema is usually not reliable, using a hack
	SELECT ORD INTO marker
	FROM rpt_mv_stats_activity WHERE AUDIT_DATE='1984-09-29';
	IF marker <> '7' THEN
		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS)
			VALUES('1984-09-29','',7,0,0,0,0);
		ALTER TABLE rpt_mv_stats_activity ADD COLUMN Android int(11);
		ALTER TABLE rpt_mv_stats_acquisition ADD COLUMN Android int(11);
	END IF;
END#

CALL alterDailyMailStats()#

DROP PROCEDURE IF EXISTS alterDailyMailStats
#

DROP VIEW IF EXISTS UNIQUE_BUYERS_YESTERDAY#
CREATE VIEW UNIQUE_BUYERS_YESTERDAY AS 
select
   distinct x.ACCOUNT_ID AS account_id,
   aid.PLATFORM AS partner_id
from EXTERNAL_TRANSACTION x 
left join rpt_players_by_platform_and_time aid on x.ACCOUNT_ID = aid.ACCOUNT_ID and aid.PLATFORM<>'*' 
	AND aid.GAME_TYPE='#' AND aid.PERIOD='day' AND (aid.AUDIT_DATE = (curdate() - interval 1 day) 
OR aid.AUDIT_DATE = (curdate() - interval 2 day)
OR aid.AUDIT_DATE = curdate() )
where x.EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND x.MESSAGE_TIMESTAMP >= (curdate() - interval 1 day) AND x.MESSAGE_TIMESTAMP < curdate()#


DROP PROCEDURE IF EXISTS fillDailyMailStats#

CREATE PROCEDURE fillDailyMailStats()
BEGIN
	declare runtime timestamp;
	
	if get_lock('strataproddw.fill_daily_mail_3', 0) = 1 then
		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS,Android)
			select Date(now()) AS AUDIT_DATE,'Users' AS ACTIVITY,0 AS ORD,
				CAST(sum(case PLATFORM when '*' then 1 else 0 end) AS UNSIGNED) as Number,
				CAST(sum(case PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS,
				CAST(sum(case PLATFORM when 'ANDROID' then 1 else 0 end) AS UNSIGNED) as Android
				from strataproddw.rpt_players_by_platform_and_time where AUDIT_DATE = (curdate() - interval 1 day)
				and GAME_TYPE='#' AND PERIOD = 'day';

		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS,Android)				
			select Date(now()) AS AUDIT_DATE,'Players' AS ACTIVITY,0 AS ORD,
				CAST(sum(case PLATFORM when '*' then 1 else 0 end) AS UNSIGNED) as Number,
				CAST(sum(case PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS,
				CAST(sum(case PLATFORM when 'ANDROID' then 1 else 0 end) AS UNSIGNED) as Android
				from strataproddw.rpt_players_by_platform_and_time where AUDIT_DATE = (curdate() - interval 1 day)
				and GAME_TYPE='*' AND PERIOD = 'day';

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS,Android)
			SELECT Date(now()) AS AUDIT_DATE,'New accounts' AS ACQUISITION,0 AS ORD,
				count(lu.player_id) as Number,
				CAST(sum(case lu.REGISTRATION_PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case lu.REGISTRATION_PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case lu.REGISTRATION_PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS,
				CAST(sum(case lu.REGISTRATION_PLATFORM when 'ANDROID' then 1 else 0 end) AS UNSIGNED) as Android
				FROM strataproddw.LOBBY_USER lu
				WHERE lu.TSREG > (cast(now() as date) - interval 1 day) and lu.TSREG < cast(now() as date);

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS,Android)
			select Date(now()) AS AUDIT_DATE,'Invitations sent' AS ACQUISITION,1 AS ORD,count(*) as Number,
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS,NULL AS Android from strataproddw.INVITATIONS
				where CREATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);

		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS,Android)
			select Date(now()) AS AUDIT_DATE,'Invitations accepted' AS ACQUISITION,2 AS ORD,count(*) as Number,
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS,NULL AS Android
				from strataproddw.INVITATIONS
				where (STATUS = 'ACCEPTED' or STATUS = 'ACCEPTED_OTHER')
				AND UPDATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);

		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS,Android)
			select Date(now()) AS AUDIT_DATE,'Purchases' AS PURCHASES,0 AS ORD,count(*) AS Number,NULL AS Facebook,
			NULL AS Yazino,NULL AS iOS,NULL AS Android
			from strataproddw.EXTERNAL_TRANSACTION
			WHERE MESSAGE_TIMESTAMP BETWEEN (cast(now() as date) - interval 1 day)
			AND cast(now() as date) AND EXTERNAL_TRANSACTION_STATUS='SUCCESS';

		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS,Android)
			select Date(now()) AS AUDIT_DATE,'Buyers' AS PURCHASES,1 AS ORD,count(DISTINCT account_id) as Number,
				CAST(sum(case PARTNER_ID when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PARTNER_ID when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PARTNER_ID when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS,
				CAST(sum(case PARTNER_ID when 'ANDROID' then 1 else 0 end) AS UNSIGNED) as Android
				from UNIQUE_BUYERS_YESTERDAY;

		REPLACE INTO rpt_mv_stats_games(AUDIT_DATE,GAME,Players,Games)
			select Date(now()) AS AUDIT_DATE,game_type AS GAME,players AS Players,games AS Games from GAME_STATS_YESTERDAY;

		REPLACE INTO rpt_mv_stats_tournaments(AUDIT_DATE,TOURNAMENT,Players,Tournaments)
			select Date(now()) AS AUDIT_DATE,GAME_TYPE AS TOURNAMENT,UNIQUE_PLAYERS AS Players,TOURNAMENTS AS Tournaments
				from UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY;

		do release_lock('strataproddw.fill_daily_mail_3');
	end if;
END
#

DROP EVENT IF EXISTS evtFillDailyMailStats#
create event if not exists evtFillDailyMailStats
on schedule every 1 day
starts curdate() + interval 1 day + interval 7 hour + interval 20 minute
DO call fillDailyMailStats()#
