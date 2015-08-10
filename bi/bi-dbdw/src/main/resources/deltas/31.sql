DROP TABLE IF EXISTS rpt_external_transaction_per_currency#
CREATE TABLE rpt_external_transaction_per_currency (
  reportDate timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  cashierName varchar(255) NOT NULL,
  results bigint(21) NOT NULL DEFAULT '0',
  currencyCode varchar(16) NOT NULL,
  amount decimal(65,4) DEFAULT NULL,
  complete int(1) DEFAULT NULL
)
#

INSERT INTO rpt_external_transaction_per_currency(reportDate,cashierName,results,currencyCode,amount,complete)
SELECT reportDate,cashierName,results,'USD',amountUsd,complete FROM rpt_external_transaction#

INSERT INTO rpt_external_transaction_per_currency(reportDate,cashierName,results,currencyCode,amount,complete)
SELECT reportDate,cashierName,0,'EUR',amountEur,complete FROM rpt_external_transaction#

INSERT INTO rpt_external_transaction_per_currency(reportDate,cashierName,results,currencyCode,amount,complete)
SELECT reportDate,cashierName,0,'GBP',amountGbp,complete FROM rpt_external_transaction#

DROP PROCEDURE IF EXISTS rptExtractExternalTransactionsPerCurrency#
CREATE PROCEDURE rptExtractExternalTransactionsPerCurrency()
BEGIN 
	DECLARE lastIncompleteDate, fromDate TIMESTAMP DEFAULT NULL; 
	SET SQL_SAFE_UPDATES=false;
	IF hour(now()) >= 2 THEN 
		SELECT MIN(reportDate) INTO @lastIncompleteDate FROM rpt_external_transaction_per_currency 
		WHERE complete = 0;  
		IF @lastIncompleteDate IS NOT NULL THEN 
			set @fromDate = @lastIncompleteDate; 
		ELSE 
			SELECT TIMESTAMP(IFNULL(MAX(reportDate), '2000-01-01'))  INTO @fromDate FROM rpt_external_transaction_per_currency; 
		END IF;  
		DELETE FROM rpt_external_transaction_per_currency where reportDate >= DATE(@fromDate);  
		INSERT rpt_external_transaction_per_currency(reportDate, cashierName, results, currencyCode,amount,complete) 
		select p.message_timestamp, p.cashier_name, count(p.auto_id), p.CURRENCY_CODE,sum(p.AMOUNT), 
			p.message_timestamp < date(now()) from EXTERNAL_TRANSACTION p 
		where month(p.message_timestamp) <> 0 and external_transaction_status='SUCCESS' 
			and p.message_timestamp >= @fromDate
		group by p.cashier_name, date(p.message_timestamp),p.CURRENCY_CODE order by p.message_timestamp desc; 
	END IF; 
END#
DROP EVENT IF EXISTS `rptExtractExternalTransactionsPerCurrency`#
CREATE EVENT `rptExtractExternalTransactionsPerCurrency` ON SCHEDULE EVERY 1 HOUR STARTS '2010-09-15 11:49:40' DO call rptExtractExternalTransactionsPerCurrency()#