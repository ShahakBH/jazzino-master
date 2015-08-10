create or replace view ad_tracking_view as
select
	pd.player_id,
	pd.created_ts::date registration_date,
	pr.registration_platform,
	pr.registration_referrer,
	et.message_ts purchase_ts,
	et.account_id,
	amount,
	et.currency_code,
	amount / rate amount_gbp,
	et.cashier_name,
	amount_chips,
	game_type,
	coalesce(platform, cashier_platform) purchase_platform
from player_definition pd inner join player_referrer pr
on pd.player_id = pr.player_id
left join external_transaction et
on pd.account_id = et.account_id
and external_transaction_status = 'SUCCESS'
left join currency_rates cr
on et.currency_code = cr.currency_code
left join cashier_platform cp
on et.cashier_name = cp.cashier_name;

GRANT SELECT ON ad_tracking_view TO GROUP READ_ONLY;
GRANT ALL ON ad_tracking_view TO GROUP READ_WRITE;
GRANT ALL ON ad_tracking_view TO GROUP SCHEMA_MANAGER;