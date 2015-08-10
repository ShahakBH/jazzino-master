DROP VIEW IF EXISTS UNIQUE_BUYERS_YESTERDAY#
CREATE VIEW UNIQUE_BUYERS_YESTERDAY AS 
select
   distinct x.ACCOUNT_ID AS account_id,
   aid.PLATFORM AS partner_id
from EXTERNAL_TRANSACTION x 
left join rpt_activity_by_account_id aid on x.ACCOUNT_ID = aid.ACCOUNT_ID and aid.PLATFORM<>'' AND (aid.AUDIT_DATE = (curdate() - interval 1 day) 
OR aid.AUDIT_DATE = (curdate() - interval 2 day)
OR aid.AUDIT_DATE = curdate() )
where x.EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND x.MESSAGE_TIMESTAMP >= (curdate() - interval 1 day) AND x.MESSAGE_TIMESTAMP < curdate()#

DROP VIEW IF EXISTS UNIQUE_BUYERS_SOURCE_YESTERDAY#
CREATE VIEW UNIQUE_BUYERS_SOURCE_YESTERDAY AS 
select
   distinct x.ACCOUNT_ID AS account_id,
   pa.REGISTRATION_PLATFORM AS partner_id
from EXTERNAL_TRANSACTION x
left join PLAYER_ACCOUNT_INFO AS pa on x.ACCOUNT_ID=pa.ACCOUNT_ID
where x.EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND x.MESSAGE_TIMESTAMP >= (curdate() - interval 1 day) AND x.MESSAGE_TIMESTAMP < curdate()#