CREATE OR REPLACE VIEW `PLAYER_PURCHASES`
AS select
     `P`.`PLAYER_ID` AS `PLAYER_ID`,
     `ET`.`AUTO_ID` AS `AUTO_ID`,
     `ET`.`MESSAGE_TIMESTAMP` AS `MESSAGE_TIMESTAMP`,
     ((`ET`.`CURRENCY_CODE` = 'USD') * `ET`.`AMOUNT`) AS `USD`,
     ((`ET`.`CURRENCY_CODE` = 'EUR') * `ET`.`AMOUNT`) AS `EUR`,
     ((`ET`.`CURRENCY_CODE` = 'GBP') * `ET`.`AMOUNT`) AS `GBP`
   from (
       `strataproddw`.`EXTERNAL_TRANSACTION` `ET`
       join `strataproddw`.`PLAYER` `P`
       join `strataproddw`.`LOBBY_USER` `PP`
   )
   where (
     (`ET`.`ACCOUNT_ID` = `P`.`ACCOUNT_ID`)
     and (`PP`.`PLAYER_ID` = `P`.`PLAYER_ID`)
     and (`ET`.`EXTERNAL_TRANSACTION_STATUS` = 'SUCCESS' OR ET.EXTERNAL_TRANSACTION_STATUS = 'AUTHORISED')
   )
#

