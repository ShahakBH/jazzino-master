--replacing the missing left join to external_transaction_mv
drop view if exists ad_tracking_view;

create view ad_tracking_view as
select
	lu.player_id,
	lu.reg_ts::date registration_date,
	lu.registration_platform,
	lu.registration_referrer,
	et.purchase_ts,
	et.account_id,
	et.amount,
	et.currency_code,
	et.amount_gbp,
	et.cashier_name,
	et.amount_chips,
	et.game_type,
	et.purchase_platform
from lobby_user lu
left join external_transaction_mv et
on lu.account_id = et.account_id;

GRANT SELECT ON ad_tracking_view TO GROUP READ_ONLY;
GRANT ALL ON ad_tracking_view TO GROUP READ_WRITE;
GRANT ALL ON ad_tracking_view TO GROUP SCHEMA_MANAGER;
