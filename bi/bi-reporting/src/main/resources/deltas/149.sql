create materialized view lifetime_value_buyers_mv as
select
    a.purchase_platform,
    a.first_purchase_month,
    a.purchase_month,
    a.total_amount_gbp,
    b.num_buyers
from (select
    purchase_platform,
    to_char(first_purchase_date, 'YYYY-MM') first_purchase_month,
    to_char(purchase_ts, 'YYYY-MM') purchase_month,
    sum(amount_gbp) total_amount_gbp,
    count(distinct player_id) num_buyers
from public.external_transaction_mv
where cashier_name in ('FACEBOOK', 'GoogleCheckout', 'PayPal', 'PayPal-WPP', 'Paypal', 'iTunes', 'PayPal DG', 'Wirecard', 'Zong', 'WorldPay', 'Amazon')
group by 1, 2, 3) a, (select
    purchase_platform,
    to_char(first_purchase_date, 'YYYY-MM') first_purchase_month,
    count(distinct player_id) num_buyers
from public.external_transaction_mv
where cashier_name::text in ('FACEBOOK', 'GoogleCheckout', 'PayPal', 'PayPal-WPP', 'Paypal', 'iTunes', 'PayPal DG', 'Wirecard', 'Zong', 'WorldPay', 'Amazon')
group by 1, 2) b
where a.purchase_platform = b.purchase_platform
and a.first_purchase_month = b.first_purchase_month;

grant select on lifetime_value_buyers_mv to group read_only;
grant all on lifetime_value_buyers_mv to group read_write;
grant all on lifetime_value_buyers_mv to group schema_manager;
