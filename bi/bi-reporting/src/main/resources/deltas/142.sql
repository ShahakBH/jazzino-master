-- WEB-4506 - fix case in purchase views

create or replace VIEW purchases_by_cashier_last_day_v AS
  select
    et.CASHIER_NAME AS cashier_name,count(1) AS num_transactions,
    count(distinct et.ACCOUNT_ID) AS num_players,
    coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '24 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'SUCCESS' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED')
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
        and (et.EXTERNAL_TRANSACTION_STATUS = 'SUCCESS' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED');

create or replace view purchases_by_cashier_last_hour_v AS
  select et.CASHIER_NAME AS cashier_name,
         count(1) AS num_transactions,count(distinct et.ACCOUNT_ID) AS num_players,
         coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '1 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'SUCCESS' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED')
  group by et.CASHIER_NAME
  union
  select 'Total' AS Total,
         count(1) AS num_transactions,
         count(distinct et.ACCOUNT_ID) AS num_players,
         coalesce(sum((et.AMOUNT / cr.RATE)), 0) AS total_amount
  from EXTERNAL_TRANSACTION et
    join CURRENCY_RATES cr on et.CURRENCY_CODE = cr.currency_code
  where et.message_ts > (now() - interval '1 hour')
        and (et.EXTERNAL_TRANSACTION_STATUS = 'SUCCESS' or et.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED');
