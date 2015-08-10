-- WEB-4296 - move Payment Options to the DB

CREATE TABLE PAYMENT_OPTION_LEVEL (
  LEVEL INT NOT NULL PRIMARY KEY,
  HEADER VARCHAR(255) NOT NULL,
  DESCRIPTION VARCHAR(255) NOT NULL,
  UPSELL_LEVEL INT NOT NULL,
  UPSELL_HEADER VARCHAR(255) NOT NULL,
  UPSELL_DESCRIPTION VARCHAR(255) NOT NULL
) charset=utf8#

INSERT INTO PAYMENT_OPTION_LEVEL (LEVEL, HEADER, DESCRIPTION, UPSELL_LEVEL, UPSELL_HEADER, UPSELL_DESCRIPTION) VALUES
  (1, 'You\'ve got STARTER STYLE!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 2, 'Be a CLEVER COMPETITOR', 'Get $CHIPS$ CHIPS for $CURRENCY$$PRICE$ more!'),
  (2, 'You\'re a CLEVER COMPETITOR!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 3, 'Take a LUCKY BREAK', 'Get $CHIPS$ CHIPS for $CURRENCY$$PRICE_DELTA$ more!'),
  (3, 'You\'ve got a LUCKY BREAK!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 4, 'Be a SAVVY STAR', 'Get $CHIPS$ CHIPS for $CURRENCY$$PRICE_DELTA$ more!'),
  (4, 'You\'re a SAVVY STAR!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 5, 'Be a POWER PLAYER', 'Get $CHIPS$ CHIPS for $CURRENCY$$PRICE_DELTA$ more!'),
  (5, 'You\'re a POWER PLAYER!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 6, 'Be a MILLIONAIRE MAVEN', 'Get $CHIPS$ CHIPS for $CURRENCY$$PRICE_DELTA$ more!'),
  (6, 'You\'re a MILLIONAIRE MAVEN!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$', 6, 'You\'re a MILLIONAIRE MAVEN!', '$CHIPS$ CHIPS for $CURRENCY$$PRICE$')#

CREATE TABLE PAYMENT_OPTION (
  PAYMENT_OPTION_ID VARCHAR(255) NOT NULL PRIMARY KEY,
  LEVEL INT NOT NULL,
  CURRENCY VARCHAR(3) DEFAULT NULL,
  PRICE DECIMAL(12, 2) NOT NULL,
  CHIPS DECIMAL(10, 0) NOT NULL,
  CURRENCY_LABEL VARCHAR(3) NOT NULL,

  FOREIGN KEY FK_PAYMENT_OPTION_LEVEL (LEVEL) REFERENCES PAYMENT_OPTION_LEVEL (LEVEL)
) charset=utf8#

INSERT INTO PAYMENT_OPTION (PAYMENT_OPTION_ID, LEVEL, CURRENCY, PRICE, CHIPS, CURRENCY_LABEL) VALUES
  ('optionUSD1', 1, 'USD', 5, 10000, '$'),
  ('optionUSD2', 2, 'USD', 10, 21000, '$'),
  ('optionUSD3', 3, 'USD', 20, 50000, '$'),
  ('optionUSD4', 4, 'USD', 50, 150000, '$'),
  ('optionUSD5', 5, 'USD', 100, 400000, '$'),
  ('optionUSD6', 6, 'USD', 150, 1000000, '$'),
  ('optionCAD1', 1, 'CAD', 5, 10000, 'C$'),
  ('optionCAD2', 2, 'CAD', 10, 21000, 'C$'),
  ('optionCAD3', 3, 'CAD', 20, 50000, 'C$'),
  ('optionCAD4', 4, 'CAD', 50, 150000, 'C$'),
  ('optionCAD5', 5, 'CAD', 100, 400000, 'C$'),
  ('optionCAD6', 6, 'CAD', 150, 1000000, 'C$'),
  ('optionAUD1', 1, 'AUD', 5, 10000, 'A$'),
  ('optionAUD2', 2, 'AUD', 10, 21000, 'A$'),
  ('optionAUD3', 3, 'AUD', 20, 50000, 'A$'),
  ('optionAUD4', 4, 'AUD', 50, 150000, 'A$'),
  ('optionAUD5', 5, 'AUD', 100, 400000, 'A$'),
  ('optionAUD6', 6, 'AUD', 150, 1000000, 'A$'),
  ('optionGBP1', 1, 'GBP', 3, 10000, '£'),
  ('optionGBP2', 2, 'GBP', 6, 21000, '£'),
  ('optionGBP3', 3, 'GBP', 12, 50000, '£'),
  ('optionGBP4', 4, 'GBP', 30, 150000, '£'),
  ('optionGBP5', 5, 'GBP', 60, 400000, '£'),
  ('optionGBP6', 6, 'GBP', 90, 1000000, '£'),
  ('optionEUR1', 1, 'EUR', 3.5, 10000, '€'),
  ('optionEUR2', 2, 'EUR', 7, 21000, '€'),
  ('optionEUR3', 3, 'EUR', 14, 50000, '€'),
  ('optionEUR4', 4, 'EUR', 35, 150000, '€'),
  ('optionEUR5', 5, 'EUR', 70, 400000, '€'),
  ('optionEUR6', 6, 'EUR', 105, 1000000, '€'),
  ('IOS_USD3', 1, 'USD', 3, 5000, '$'),
  ('IOS_USD8', 2, 'USD', 8, 15000, '$'),
  ('IOS_USD15', 3, 'USD', 15, 30000, '$'),
  ('IOS_USD30', 4, 'USD', 30, 70000, '$'),
  ('IOS_USD70', 5, 'USD', 70, 200000, '$'),
  ('IOS_USD90', 6, 'USD', 90, 300000, '$')#

CREATE TABLE PAYMENT_OPTION_PLATFORM (
  PLATFORM VARCHAR(32) NOT NULL,
  PAYMENT_OPTION_ID VARCHAR(255) NOT NULL,

  PRIMARY KEY (PLATFORM, PAYMENT_OPTION_ID),
  KEY IDX_PAYMENT_OPTION_PLATFORM (PLATFORM),
  FOREIGN KEY FK_PAYMENT_OPTION_PLATFORM_OPTION (PAYMENT_OPTION_ID) REFERENCES PAYMENT_OPTION (PAYMENT_OPTION_ID)
) charset=utf8#

INSERT INTO PAYMENT_OPTION_PLATFORM (PLATFORM, PAYMENT_OPTION_ID) VALUES
  ('WEB', 'optionUSD1'),
  ('WEB', 'optionUSD2'),
  ('WEB', 'optionUSD3'),
  ('WEB', 'optionUSD4'),
  ('WEB', 'optionUSD5'),
  ('WEB', 'optionUSD6'),
  ('WEB', 'optionCAD1'),
  ('WEB', 'optionCAD2'),
  ('WEB', 'optionCAD3'),
  ('WEB', 'optionCAD4'),
  ('WEB', 'optionCAD5'),
  ('WEB', 'optionCAD6'),
  ('WEB', 'optionAUD1'),
  ('WEB', 'optionAUD2'),
  ('WEB', 'optionAUD3'),
  ('WEB', 'optionAUD4'),
  ('WEB', 'optionAUD5'),
  ('WEB', 'optionAUD6'),
  ('WEB', 'optionGBP1'),
  ('WEB', 'optionGBP2'),
  ('WEB', 'optionGBP3'),
  ('WEB', 'optionGBP4'),
  ('WEB', 'optionGBP5'),
  ('WEB', 'optionGBP6'),
  ('WEB', 'optionEUR1'),
  ('WEB', 'optionEUR2'),
  ('WEB', 'optionEUR3'),
  ('WEB', 'optionEUR4'),
  ('WEB', 'optionEUR5'),
  ('WEB', 'optionEUR6'),
  ('IOS', 'IOS_USD3'),
  ('IOS', 'IOS_USD8'),
  ('IOS', 'IOS_USD15'),
  ('IOS', 'IOS_USD30'),
  ('IOS', 'IOS_USD70'),
  ('IOS', 'IOS_USD90'),
  ('ANDROID', 'IOS_USD3'),
  ('ANDROID', 'IOS_USD8'),
  ('ANDROID', 'IOS_USD15'),
  ('ANDROID', 'IOS_USD30'),
  ('ANDROID', 'IOS_USD70'),
  ('ANDROID', 'IOS_USD90')#
