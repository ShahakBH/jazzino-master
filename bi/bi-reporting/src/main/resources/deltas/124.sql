-- WEB-4398 - fix purchases with offers view for WorldPay
-- Hotfix applied to prod 14/10/2013

drop view purchase_with_offers_view;
create view purchase_with_offers_view as
  select
    pd.player_id,
    pd.account_id,
    pd.created_ts::date registration_date,
    pr.registration_platform,
    pr.registration_referrer,
    min(message_ts::date) over (partition by et.account_id) first_purchase_date,
et.message_ts purchase_ts,
amount,
et.currency_code,
amount / rate amount_gbp,
et.cashier_name,
et.promo_id,
amount_chips,
transaction_type,
game_type,
coalesce(platform, cashier_platform) purchase_platform,
rank() over (partition by et.account_id order by message_ts) purchase_num
from external_transaction et, currency_rates cr, player_definition pd, player_referrer pr, cashier_platform cp
where et.currency_code = cr.currency_code
and et.account_id = pd.account_id
and pd.player_id = pr.player_id
and (external_transaction_status = 'SUCCESS' or external_transaction_status = 'AUTHORISED')
and et.cashier_name = cp.cashier_name;

GRANT SELECT ON purchase_with_offers_view TO GROUP READ_ONLY;
GRANT ALL ON purchase_with_offers_view TO GROUP READ_WRITE;
GRANT ALL ON purchase_with_offers_view TO GROUP SCHEMA_MANAGER;
