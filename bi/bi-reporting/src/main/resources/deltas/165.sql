GRANT SELECT ON external_transaction_mv TO GROUP READ_ONLY;
GRANT ALL ON external_transaction_mv TO GROUP READ_WRITE;
GRANT ALL ON external_transaction_mv TO GROUP ADMIN;
GRANT ALL ON external_transaction_mv TO GROUP reporting;
GRANT ALL ON external_transaction_mv TO GROUP SCHEMA_MANAGER;

GRANT SELECT ON player_purchase_daily_by_game TO GROUP read_only;
GRANT ALL ON player_purchase_daily_by_game TO GROUP read_write;
GRANT ALL ON player_purchase_daily_by_game TO GROUP schema_manager;
