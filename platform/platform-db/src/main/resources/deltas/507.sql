insert ignore into PAYMENT_OPTION (PAYMENT_OPTION_ID, LEVEL, CURRENCY, PRICE, CHIPS, FX_PAYMENT_OPTION_ID)
values ('option2_7_99USD', 2, 'USD', 7.99, 15000, NULL)
#

insert ignore INTO `PAYMENT_OPTION_PLATFORM` (`PLATFORM`, `PAYMENT_OPTION_ID`)
VALUES
('AMAZON', 'option2_7_99USD'),
('ANDROID', 'option2_7_99USD')
#

delete from PAYMENT_OPTION_PLATFORM where payment_option_id = 'option2_8_99USD'
#

delete from PAYMENT_OPTION where payment_option_id = 'option2_8_99USD'
#
