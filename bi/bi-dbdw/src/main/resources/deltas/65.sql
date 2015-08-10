DROP PROCEDURE IF EXISTS external_transactions_inserts#
CREATE PROCEDURE external_transactions_inserts(IN account_id_val INT(11), IN first_purchase_val DATETIME)
BEGIN
  INSERT INTO PLAYER_ACCOUNT_INFO(ACCOUNT_ID, FIRST_PURCHASE)
    VALUES (account_id_val,first_purchase_val)
      ON DUPLICATE KEY
      UPDATE FIRST_PURCHASE = 
          IF(FIRST_PURCHASE IS NULL,VALUES(FIRST_PURCHASE),FIRST_PURCHASE);
END
#

DROP TRIGGER IF EXISTS external_transactions_actions#
CREATE TRIGGER external_transactions_actions
AFTER INSERT ON EXTERNAL_TRANSACTION
FOR EACH ROW
BEGIN
  DECLARE player_id_val BIGINT(20) DEFAULT 0;

  CALL external_transactions_inserts(NEW.ACCOUNT_ID,NEW.MESSAGE_TIMESTAMP);
END#
