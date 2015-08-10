DROP VIEW IF EXISTS UNIQUE_PLAYERS_YESTERDAY#
CREATE VIEW UNIQUE_PLAYERS_YESTERDAY AS 
select
   distinct a.PLAYER_ID AS player_id,
   aid.PLATFORM AS partner_id
from rpt_account_activity a 
left join strataprod.PLAYER pp on pp.PLAYER_ID = a.PLAYER_ID
left join rpt_activity_by_account_id aid on pp.ACCOUNT_ID = aid.ACCOUNT_ID and aid.PLATFORM<>'' AND (aid.AUDIT_DATE = (curdate() - interval 1 day) 
OR aid.AUDIT_DATE = (curdate() - interval 2 day)
OR aid.AUDIT_DATE = curdate() )
where ((a.AUDIT_DATE = (curdate() - interval 1 day)) and (a.GAME_TYPE <> ''))#

DROP VIEW IF EXISTS UNIQUE_PLAYERS_SOURCE_YESTERDAY#
CREATE VIEW UNIQUE_PLAYERS_SOURCE_YESTERDAY AS 
select
   distinct a.PLAYER_ID AS player_id,
   pa.REGISTRATION_PLATFORM AS partner_id
from rpt_account_activity a 
left join PLAYER_INFO AS pa on a.PLAYER_ID=pa.PLAYER_ID
where ((a.AUDIT_DATE = (curdate() - interval 1 day)) and (a.GAME_TYPE <> ''))#

DROP VIEW IF EXISTS UNIQUE_USERS_YESTERDAY#
CREATE VIEW UNIQUE_USERS_YESTERDAY AS 
select
   distinct a.ACCOUNT_ID AS ACCOUNT_ID,
   pa.REGISTRATION_PLATFORM AS PLATFORM
from rpt_activity_by_account_id a 
left join PLAYER_ACCOUNT_INFO AS pa on a.ACCOUNT_ID=pa.ACCOUNT_ID
where a.AUDIT_DATE = (curdate() - interval 1 day) and a.PLATFORM <> ''#

DROP VIEW IF EXISTS UNIQUE_BUYERS_YESTERDAY#
CREATE VIEW UNIQUE_BUYERS_YESTERDAY AS 
select
   distinct x.ACCOUNT_ID AS account_id,
   aid.PLATFORM AS partner_id
from EXTERNAL_TRANSACTION x 
left join rpt_activity_by_account_id aid on x.ACCOUNT_ID = aid.ACCOUNT_ID and aid.PLATFORM<>'' AND (aid.AUDIT_DATE = (curdate() - interval 1 day) 
OR aid.AUDIT_DATE = (curdate() - interval 2 day)
OR aid.AUDIT_DATE = curdate() )
where x.MESSAGE_TIMESTAMP >= (curdate() - interval 1 day) AND x.MESSAGE_TIMESTAMP < curdate()#

DROP VIEW IF EXISTS UNIQUE_BUYERS_SOURCE_YESTERDAY#
CREATE VIEW UNIQUE_BUYERS_SOURCE_YESTERDAY AS 
select
   distinct x.ACCOUNT_ID AS account_id,
   pa.REGISTRATION_PLATFORM AS partner_id
from EXTERNAL_TRANSACTION x
left join PLAYER_ACCOUNT_INFO AS pa on x.ACCOUNT_ID=pa.ACCOUNT_ID
where x.MESSAGE_TIMESTAMP >= (curdate() - interval 1 day) AND x.MESSAGE_TIMESTAMP < curdate()#


