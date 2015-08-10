SET SQL_SAFE_UPDATES=0#

-- UPDATE EXTERNAL_TRANSACTION x, strataprod.ZONG_TRANSACTION z
-- SET x.AMOUNT=z.CONSUMERPRICE, x.CURRENCY_CODE=z.CONSUMERCURRENCY,
-- x.MESSAGE_TIMESTAMP=x.MESSAGE_TIMESTAMP
-- WHERE x.CASHIER_NAME='Zong' AND x.INTERNAL_TRANSACTION_ID=z.ID#

DELETE FROM rpt_external_transaction_per_currency where cashierName='Zong'#
INSERT rpt_external_transaction_per_currency(reportDate, cashierName, results, currencyCode,amount,complete) 
select p.message_timestamp, p.cashier_name, count(p.auto_id), p.CURRENCY_CODE,sum(p.AMOUNT), 
	p.message_timestamp < date(now()) from EXTERNAL_TRANSACTION p 
  where cashier_name='Zong'
group by p.cashier_name, date(p.message_timestamp),p.CURRENCY_CODE order by p.message_timestamp desc#
