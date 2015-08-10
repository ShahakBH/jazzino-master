-- WEB-4326 - increase exchange rate precision to match WorldPay

ALTER TABLE PAYMENT_SETTLEMENT MODIFY COLUMN EXCHANGE_RATE DECIMAL(13,7)#
