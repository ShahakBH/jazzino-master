alter table player_level drop constraint player_level_pkey;
alter table player_level add primary key(PLAYER_ID, GAME_TYPE);
