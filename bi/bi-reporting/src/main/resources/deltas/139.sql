-- WEB-4513 - cashier fixes, Amazon support

insert into cashier_platform (cashier_name,cashier_platform) values ('Amazon', 'AMAZON');

create or replace VIEW purchases_by_cashier_last_day_v AS
  select
    et.CASHIER_NAME AS cashier_name,count(1) AS num_transactions,
    count(distinct et.ACCOUNT_ID) AS num_players,
    coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '24 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'Success' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED')
  group by et.CASHIER_NAME
  union
  select
    'Total' AS Total,
    count(1) AS num_transactions,
    count(distinct et.ACCOUNT_ID) AS num_players,
    coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '24 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'Success' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED');

create or replace view purchases_by_cashier_last_hour_v AS
  select et.CASHIER_NAME AS cashier_name,
         count(1) AS num_transactions,count(distinct et.ACCOUNT_ID) AS num_players,
         coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '1 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'Success' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED')
  group by et.CASHIER_NAME
  union
  select 'Total' AS Total,
         count(1) AS num_transactions,
         count(distinct et.ACCOUNT_ID) AS num_players,
         coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '1 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'Success' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED');

GRANT SELECT ON purchases_by_cashier_last_hour_v,purchases_by_cashier_last_day_v TO GROUP READ_ONLY;
GRANT ALL ON purchases_by_cashier_last_hour_v,purchases_by_cashier_last_day_v TO GROUP READ_WRITE, SCHEMA_MANAGER;

create or replace view external_transaction_view as
  select
    pd.player_id,
    pd.account_id,
    pd.created_ts::date registration_date,
    lu.display_name,
    lu.country registration_country,
    pr.registration_platform,
    pr.registration_referrer,
    min(message_ts::date) over (partition by et.account_id) first_purchase_date,
et.message_ts purchase_ts,
amount,
et.currency_code,
amount / rate amount_gbp,
et.cashier_name,
et.external_transaction_status,
amount_chips,
transaction_type,
et.promo_id,
game_type
from external_transaction et, currency_rates cr, player_definition pd, player_referrer pr, lobby_user lu
where et.currency_code = cr.currency_code
and et.account_id = pd.account_id
and pd.player_id = pr.player_id
and pr.player_id = lu.player_id
and et.cashier_name in ('FACEBOOK', 'GoogleCheckout', 'PayPal', 'PayPal-WPP', 'Paypal', 'iTunes', 'PayPal DG', 'Wirecard', 'Zong', 'WorldPay', 'Amazon');

