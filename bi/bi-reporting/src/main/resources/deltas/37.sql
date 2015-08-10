create or replace view purchase_view as
select
	pd.player_id,
	pd.created_ts::date,
	pr.registration_platform,
	pr.registration_referrer,
	et.*,
	rank() over (partition by et.account_id order by message_ts) purchase_num
from external_transaction et, player_definition pd, player_referrer pr
where et.account_id = pd.account_id
and pd.player_id = pr.player_id
and external_transaction_status = 'SUCCESS';