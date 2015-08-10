create table last_played_mv (
player_id DECIMAL(16,2) not null distkey sortkey PRIMARY KEY,
last_played_ts timestamp without time zone not null,
FOREIGN KEY(PLAYER_ID) REFERENCES LOBBY_USER(PLAYER_ID)
);

GRANT SELECT ON last_played_mv TO GROUP READ_ONLY;
GRANT ALL ON last_played_mv TO GROUP READ_WRITE;
GRANT ALL ON last_played_mv TO GROUP SCHEMA_MANAGER;
