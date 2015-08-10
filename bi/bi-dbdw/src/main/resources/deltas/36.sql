-- update the external transaction report table

DELETE FROM rpt_external_transaction_per_currency where cashierName='iTunes'#

INSERT rpt_external_transaction_per_currency(reportDate, cashierName, results, currencyCode,amount,complete)
select p.message_timestamp, p.cashier_name, count(p.auto_id), p.CURRENCY_CODE,sum(p.AMOUNT),
    p.message_timestamp < date(now()) from EXTERNAL_TRANSACTION p
where p.cashier_name='iTunes' and month(p.message_timestamp) <> 0 and external_transaction_status='SUCCESS'
group by p.cashier_name, date(p.message_timestamp),p.CURRENCY_CODE order by p.message_timestamp desc#
