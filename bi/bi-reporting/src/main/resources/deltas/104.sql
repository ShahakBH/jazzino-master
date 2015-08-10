CREATE TABLE scheduled_aggregators(
  aggregator varchar(64) NOT NULL SORTKEY DISTKEY
)
;
GRANT SELECT ON scheduled_aggregators TO GROUP READ_ONLY;
GRANT ALL ON scheduled_aggregators TO GROUP READ_WRITE;
GRANT ALL ON scheduled_aggregators TO GROUP SCHEMA_MANAGER;

INSERT INTO maintenance (table_name, key_columns, vacuumed, table_analysed, keys_analysed) VALUES
  ('scheduled_aggregators', 'aggregator','2011-01-01 11:11:11','2011-01-01 11:11:11','2011-01-01 11:11:11');
