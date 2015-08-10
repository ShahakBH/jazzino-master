drop table if exists stg_lockout_bonus;
create table if not exists stg_lockout_bonus(
      PLAYER_ID decimal(16,2) NOT NULL PRIMARY KEY,
      LAST_BONUS_TS timestamp NOT NULL
);

CREATE INDEX ON stg_lockout_bonus (last_bonus_ts);
CREATE INDEX ON stg_lockout_bonus (player_id);

GRANT SELECT ON stg_lockout_bonus TO GROUP READ_ONLY;
GRANT ALL ON stg_lockout_bonus TO GROUP READ_WRITE;
GRANT ALL ON stg_lockout_bonus TO GROUP SCHEMA_MANAGER;
--

drop table if exists lockout_bonus;
create table if not exists lockout_bonus(
      PLAYER_ID decimal(16,2) NOT NULL PRIMARY KEY,
      LAST_BONUS_TS timestamp NOT NULL
);

CREATE INDEX ON lockout_bonus (last_bonus_ts);
CREATE INDEX ON lockout_bonus (player_id);

GRANT SELECT ON lockout_bonus TO GROUP READ_ONLY;
GRANT ALL ON lockout_bonus TO GROUP READ_WRITE;
GRANT ALL ON lockout_bonus TO GROUP SCHEMA_MANAGER;
--
