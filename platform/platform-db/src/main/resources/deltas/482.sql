-- WEB-4326 - add table to track transactions requiring settlement

CREATE TABLE PAYMENT_SETTLEMENT (
  INTERNAL_TRANSACTION_ID VARCHAR(255) NOT NULL PRIMARY KEY,
  EXTERNAL_TRANSACTION_ID VARCHAR(255) NOT NULL,
  ACCOUNT_ID DECIMAL(16,2) NOT NULL,
  CASHIER_NAME VARCHAR(255) NOT NULL,
  TRANSACTION_TS TIMESTAMP NOT NULL,
  ACCOUNT_NUMBER VARCHAR(16),
  PRICE DECIMAL(64,4) NOT NULL,
  CURRENCY_CODE VARCHAR(3) NOT NULL,
  CHIPS DECIMAL(64,4) NOT NULL,
  TRANSACTION_TYPE VARCHAR(32) NOT NULL,
  GAME_TYPE VARCHAR(255),
  PLATFORM VARCHAR(32),
  PAYMENT_OPTION_ID VARCHAR(128),
  BASE_CURRENCY_AMOUNT DECIMAL(64,4),
  BASE_CURRENCY_CODE VARCHAR(3),
  EXCHANGE_RATE DECIMAL(12,6)
) charset=utf8#

CREATE INDEX IDX_PAYMENT_SETTLEMENT_TRANSACTION_TS ON PAYMENT_SETTLEMENT(TRANSACTION_TS)#
