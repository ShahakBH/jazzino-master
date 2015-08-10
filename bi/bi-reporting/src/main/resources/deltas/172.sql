drop table if exists day_zero;
create table day_zero (
 player_id numeric(16,2) not null,
 reg_ts timestamp without time zone not null,
 level integer,
 account_id numeric(16,2) not null,
 sessions bigint,
 payout numeric,
 bonus_collections  integer,
 balance   numeric(64,4),
 registration_platform   character varying(32) not null,
 registration_game_type  character varying(32) not null,
 stakes integer
);

create unique index on day_zero(player_id);
create index on day_zero(reg_ts);

grant select on day_zero to group read_only;
grant all on day_zero to group read_write;
grant all on day_zero to group schema_manager;
