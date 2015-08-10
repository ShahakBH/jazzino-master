DROP VIEW IF EXISTS `strataproddw`.`mm_external_transactions`#

CREATE VIEW `mm_external_transactions`
AS select
   `ET`.`AUTO_ID` AS `AUTO_ID`,
   `lu`.`PROVIDER_NAME` AS `PARTNER_ID`,
   `lu`.`EXTERNAL_ID` AS `EXTERNAL_ID`,
   `lu`.`USER_ID` AS `USER_ID`,
   `ET`.`GAME_TYPE` AS `GAME_TYPE`,
   `ET`.`AMOUNT_CHIPS` AS `AMOUNT_CHIPS`,
   `ET`.`AMOUNT` AS `AMOUNT`,
   `ET`.`CURRENCY_CODE` AS `CURRENCY_CODE`,
   `ET`.`TRANSACTION_TYPE` AS `TRANSACTION_TYPE`,
   `ET`.`MESSAGE_TIMESTAMP` AS `TIMESTAMP`,
   `ET`.`EXTERNAL_TRANSACTION_STATUS` AS `EXTERNAL_TRANSACTION_STATUS`
from ((`strataproddw`.`EXTERNAL_TRANSACTION` `ET` join `strataproddw`.`PLAYER` `P`) left join `strataproddw`.`LOBBY_USER` `lu` on((`P`.`PLAYER_ID` = `lu`.`PLAYER_ID`)))
left join `strataproddw`.`PROVIDER_REJECTED_EMAIL_ADDRESSES` `pre` on (`P`.`PLAYER_ID` = `pre`.`PLAYER_ID`)
where ((`ET`.`EXTERNAL_TRANSACTION_STATUS` = 'SUCCESS') and (`P`.`ACCOUNT_ID` = `ET`.`ACCOUNT_ID`))
AND `pre`.`PLAYER_ID` is null#


alter table LAST_PLAYED add index (last_played)#
