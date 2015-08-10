create or replace view player_purchase_daily as
select
	pd.player_id,
	pd.created_ts::date registration_date,
	coalesce(platform, cashier_platform) purchase_platform,
	message_ts::date purchase_date,
	sum(amount / rate) total_amount_gbp,
	count(1) num_purchases
from external_transaction et, currency_rates cr, player_definition pd, cashier_platform cp
where et.currency_code = cr.currency_code
and et.account_id = pd.account_id
and external_transaction_status = 'SUCCESS'
and et.cashier_name = cp.cashier_name
and et.cashier_name in ('FACEBOOK', 'GoogleCheckout', 'PayPal', 'PayPal-WPP', 'Paypal', 'iTunes', 'PayPal DG', 'Wirecard', 'Zong')
group by 1, 2, 3, 4;

GRANT SELECT ON player_purchase_daily TO GROUP READ_ONLY;
GRANT ALL ON player_purchase_daily TO GROUP READ_WRITE;
GRANT ALL ON player_purchase_daily TO GROUP SCHEMA_MANAGER;