CREATE TABLE engagement_by_platform (
  PLAYED_TS timestamp SORTKEY DISTKEY PRIMARY KEY,
  PLATFORM varchar(32),
  GAME_VARIATION_TEMPLATE_NAME varchar(32),
  TRANSACTION_TYPE varchar(32),
  NUM_PLAYERS integer,
  NUM_TRANSACTIONS integer,
  total_amount decimal(16,2)
);

GRANT SELECT ON engagement_by_platform TO GROUP READ_ONLY;
GRANT ALL ON engagement_by_platform TO GROUP READ_WRITE;
GRANT ALL ON engagement_by_platform TO GROUP SCHEMA_MANAGER;
