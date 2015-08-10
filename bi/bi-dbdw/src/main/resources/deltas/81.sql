CREATE TABLE IF NOT EXISTS rpt_invites_activity (
  PLAYER_ID bigint(20) not null,
  AUDIT_DATE date not null,
  INVITES_SENT int(11) not null,
  primary key (PLAYER_ID, AUDIT_DATE),
  key IDX_AUDIT_DATE (AUDIT_DATE)
)#

DROP PROCEDURE IF EXISTS fillInvitesSummary#
CREATE PROCEDURE fillInvitesSummary()
BEGIN
	declare runtime timestamp;

	if get_lock('strataproddw.fill_invites_summary', 0) = 1 then
		SELECT DATE(DATE_SUB(MAX(AUDIT_DATE), INTERVAL 1 DAY)) INTO @runtime FROM rpt_invites_activity;
	
		INSERT IGNORE INTO rpt_invites_activity(PLAYER_ID,AUDIT_DATE,INVITES_SENT)
			SELECT PLAYER_ID,DATE(CREATED_TS),COUNT(*)
			FROM INVITATIONS
			WHERE CREATED_TS >= @runtime AND CREATED_TS < date(now())
			GROUP BY PLAYER_ID,DATE(CREATED_TS);
	
		do release_lock('strataproddw.fill_invites_summary');
	end if;
END#

DROP EVENT IF EXISTS evtFillInvitesSummary#
CREATE EVENT evtFillInvitesSummary ON SCHEDULE EVERY 1 DAY STARTS '2011-12-18 04:00:00' DO call fillInvitesSummary()#

CREATE TABLE IF NOT EXISTS rpt_mv_stats_activity (
  AUDIT_DATE date not null,
  ACTIVITY varchar(64) not null,
  ORD int(11) not null,
  Number bigint(20) null,
  Facebook bigint(20) null,
  Yazino bigint(20) null,
  iOS bigint(20) null,
  primary key (AUDIT_DATE,ACTIVITY)
)#

CREATE TABLE IF NOT EXISTS rpt_mv_stats_acquisition (
  AUDIT_DATE date not null,
  ACQUISITION varchar(64) not null,
  ORD int(11) not null,
  Number bigint(20) null,
  Facebook bigint(20) null,
  Yazino bigint(20) null,
  iOS bigint(20) null,
  primary key (AUDIT_DATE,ACQUISITION)
)#

CREATE TABLE IF NOT EXISTS rpt_mv_stats_purchases (
  AUDIT_DATE date not null,
  PURCHASES varchar(64) not null,
  ORD int(11) not null,
  Number bigint(20) null,
  Facebook bigint(20) null,
  Yazino bigint(20) null,
  iOS bigint(20) null,
  primary key (AUDIT_DATE,PURCHASES)
)#

CREATE TABLE IF NOT EXISTS rpt_mv_stats_games (
  AUDIT_DATE date not null,
  GAME varchar(64) not null,
  Players bigint(20) null,
  Games bigint(20) null,
  primary key (AUDIT_DATE,GAME)
)#

CREATE TABLE IF NOT EXISTS rpt_mv_stats_tournaments (
  AUDIT_DATE date not null,
  TOURNAMENT varchar(64) not null,
  Players bigint(20) null,
  Tournaments bigint(20) null,
  primary key (AUDIT_DATE,TOURNAMENT)
)#


DROP PROCEDURE IF EXISTS fillDailyMailStats#
CREATE PROCEDURE fillDailyMailStats()
BEGIN
	declare runtime timestamp;
	
	if get_lock('strataproddw.fill_daily_mail_2', 0) = 1 then
		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Users' AS ACTIVITY,0 AS ORD,count(DISTINCT ACCOUNT_ID) as Number,
				CAST(sum(case PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS 
				from rpt_activity_by_account_id where AUDIT_DATE = (curdate() - interval 1 day);
				
		REPLACE INTO rpt_mv_stats_activity(AUDIT_DATE,ACTIVITY,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Players' AS ACTIVITY,1 AS ORD,count(DISTINCT PLAYER_ID) as Number,
				CAST(sum(case PARTNER_ID when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PARTNER_ID when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PARTNER_ID when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS 
				from UNIQUE_PLAYERS_YESTERDAY;
				
		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			SELECT Date(now()) AS AUDIT_DATE,'New accounts' AS ACQUISITION,0 AS ORD,count(p.player_id) as Number,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case pi.REGISTRATION_PLATFORM when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS 
				FROM strataprod.PLAYER p 
				LEFT JOIN strataproddw.PLAYER_ACCOUNT_INFO pi ON p.ACCOUNT_ID=pi.ACCOUNT_ID 
				WHERE p.tscreated > (cast(now() as date) - interval 1 day) and p.tscreated < cast(now() as date);
				
		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Invitations sent' AS ACQUISITION,1 AS ORD,count(*) as Number, 
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS from strataproddw.INVITATIONS 
				where CREATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);
		
		REPLACE INTO rpt_mv_stats_acquisition(AUDIT_DATE,ACQUISITION,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Invitations accepted' AS ACQUISITION,2 AS ORD,count(*) as Number, 
				CAST(sum(case INVITED_FROM when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				NULL AS Yazino,NULL AS iOS 
				from strataproddw.INVITATIONS 
				where (STATUS = 'ACCEPTED' or STATUS = 'ACCEPTED_OTHER') 
				AND UPDATED_TS BETWEEN (cast(now() as date) - interval 1 day) and cast(now() as date);
				
		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Purchases' AS PURCHASES,0 AS ORD,count(*) AS Number,NULL AS Facebook,NULL AS Yazino,NULL AS iOS 
			from strataproddw.EXTERNAL_TRANSACTION 
			WHERE MESSAGE_TIMESTAMP BETWEEN (cast(now() as date) - interval 1 day) 
			AND cast(now() as date) AND EXTERNAL_TRANSACTION_STATUS='SUCCESS';
			
		REPLACE INTO rpt_mv_stats_purchases(AUDIT_DATE,PURCHASES,ORD,Number,Facebook,Yazino,iOS)
			select Date(now()) AS AUDIT_DATE,'Buyers' AS PURCHASES,1 AS ORD,count(DISTINCT account_id) as Number,
				CAST(sum(case PARTNER_ID when 'FACEBOOK' then 1 else 0 end) AS UNSIGNED) as Facebook,
				CAST(sum(case PARTNER_ID when 'YAZINO_WEB' then 1 else 0 end) AS UNSIGNED) as Yazino,
				CAST(sum(case PARTNER_ID when 'MOBILE' then 1 else 0 end) AS UNSIGNED) as iOS 
				from UNIQUE_BUYERS_YESTERDAY;
				
		REPLACE INTO rpt_mv_stats_games(AUDIT_DATE,GAME,Players,Games)
			select Date(now()) AS AUDIT_DATE,game_type AS GAME,players AS Players,games AS Games from GAME_STATS_YESTERDAY;
			
		REPLACE INTO rpt_mv_stats_tournaments(AUDIT_DATE,TOURNAMENT,Players,Tournaments)
			select Date(now()) AS AUDIT_DATE,GAME_TYPE AS TOURNAMENT,UNIQUE_PLAYERS AS Players,TOURNAMENTS AS Tournaments 
				from UNIQUE_PLAYERS_TOURNAMENT_YESTERDAY;
				
		do release_lock('strataproddw.fill_daily_mail_2');
	end if;
END#

DROP EVENT IF EXISTS evtFillDailyMailStats#
CREATE EVENT evtFillDailyMailStats ON SCHEDULE EVERY 1 DAY STARTS '2011-12-24 02:30:00' DO call fillDailyMailStats()#
