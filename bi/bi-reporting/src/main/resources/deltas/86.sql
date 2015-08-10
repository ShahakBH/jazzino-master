ALTER table STG_PLAYER_ACTIVITY_HOURLY
add column last_played_ts timestamp;

ALTER table PLAYER_ACTIVITY_HOURLY
add column last_played_ts timestamp;

