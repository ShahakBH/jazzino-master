--fixes bug wheere player_events were trying to override defaults in the lobby table
alter table stg_lobby_user alter column partner_id set default 'PLAY_FOR_FUN';
