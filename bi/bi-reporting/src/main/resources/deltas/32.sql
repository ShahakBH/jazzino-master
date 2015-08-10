CREATE TABLE leaderboard_position (
  leaderboard_id DECIMAL(16,2) NOT NULL,
  player_id DECIMAL(16,2) DISTKEY SORTKEY,
  leaderboard_position integer,
  PRIMARY KEY(leaderboard_id, player_id),
  FOREIGN KEY(PLAYER_ID) REFERENCES LOBBY_USER(PLAYER_ID)
);

GRANT SELECT ON leaderboard_position TO GROUP READ_ONLY;
GRANT ALL ON leaderboard_position TO GROUP READ_WRITE;
GRANT ALL ON leaderboard_position TO GROUP SCHEMA_MANAGER;
