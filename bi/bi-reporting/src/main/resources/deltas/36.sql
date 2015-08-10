-- for got to put some grants on some tables best to use delta to put them back on
GRANT SELECT ON player_activity_daily TO GROUP READ_ONLY;
GRANT ALL ON player_activity_daily TO GROUP READ_WRITE;
GRANT ALL ON player_activity_daily TO GROUP SCHEMA_MANAGER;

GRANT SELECT ON aggregator_last_update TO GROUP READ_ONLY;
GRANT ALL ON aggregator_last_update TO GROUP READ_WRITE;
GRANT ALL ON aggregator_last_update TO GROUP SCHEMA_MANAGER;