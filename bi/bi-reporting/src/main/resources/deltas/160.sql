-- drop views that depend on player

DROP VIEW IF EXISTS registrations;

-- recreate player adding partner_id

DROP VIEW IF EXISTS player;
CREATE VIEW player
(
    player_id,
    account_id,
    balance,
    registration_date,
    registration_platform,
    registration_game_type,
    partner_id,
    picture_location,
    country
)
AS
  SELECT u.player_id,
    u.account_id,
    a.balance,
    u.reg_ts AS registration_date,
    u.registration_platform,
    u.registration_game_type,
    u.partner_id,
    u.picture_location,
    u.country
  FROM lobby_user u
    LEFT JOIN account a ON a.account_id = u.account_id;

GRANT SELECT ON player TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON player TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON player TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON player TO reporting;

-- recreate registrations (from 143.sql)

CREATE VIEW registrations
(
    registration_date,
    registration_platform,
    num_registrations
)
AS
  SELECT player.registration_date::date AS registration_date,
    player.registration_platform,
         count(1) AS num_registrations
  FROM player
  GROUP BY player.registration_date::date, player.registration_platform;

GRANT SELECT ON registrations TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON registrations TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON registrations TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON registrations TO reporting;
