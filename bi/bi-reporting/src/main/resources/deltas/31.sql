CREATE TABLE leaderboard (
  LEADERBOARD_ID DECIMAL(16,2) NOT NULL DISTKEY PRIMARY KEY,
  game_type character varying(255),
  end_ts timestamp without time zone SORTKEY
);

GRANT SELECT ON leaderboard TO GROUP READ_ONLY;
GRANT ALL ON leaderboard TO GROUP READ_WRITE;
GRANT ALL ON leaderboard TO GROUP SCHEMA_MANAGER;
