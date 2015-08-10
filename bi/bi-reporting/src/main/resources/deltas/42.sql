DROP TABLE player_activity_daily;

CREATE TABLE player_activity_daily (
PLAYER_ID DECIMAL(16,2) NOT NULL DISTKEY,
GAME varchar(255),
PLATFORM varchar(255),
ACTIVITY_TS timestamp without time zone DEFAULT SYSDATE SORTKEY,
REFERRER varchar(255),
REG_TS timestamp without time zone DEFAULT SYSDATE
);

GRANT SELECT ON player_activity_daily TO GROUP READ_ONLY;
GRANT ALL ON player_activity_daily TO GROUP READ_WRITE;
GRANT ALL ON player_activity_daily TO GROUP SCHEMA_MANAGER;
