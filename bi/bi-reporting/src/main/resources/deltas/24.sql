CREATE view successful_transactions_view as
SELECT * FROM EXTERNAL_TRANSACTION
WHERE external_transaction_status='SUCCESS';

GRANT SELECT ON successful_transactions_view TO GROUP READ_ONLY;
GRANT ALL ON successful_transactions_view TO GROUP READ_WRITE;
GRANT ALL ON successful_transactions_view TO GROUP SCHEMA_MANAGER;
