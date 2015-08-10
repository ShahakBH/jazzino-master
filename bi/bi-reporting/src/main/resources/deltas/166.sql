DROP TABLE IF EXISTS MOBILE_DEVICE_HISTORY;
DROP TABLE IF EXISTS MOBILE_DEVICE;

CREATE TABLE MOBILE_DEVICE (
  ID bigserial NOT NULL primary key,
  PLAYER_ID decimal(16,2) NOT NULL,
  GAME_TYPE varchar(255) NOT NULL,
  PLATFORM varchar(255) NOT NULL,
  APP_ID varchar(128),
  DEVICE_ID varchar(128),
  PUSH_TOKEN varchar,
  ACTIVE boolean default true
);

CREATE INDEX ON mobile_device (player_id, game_type, platform);
CREATE INDEX ON mobile_device (device_id);
CREATE INDEX ON mobile_device (push_token);

grant select on MOBILE_DEVICE to group read_only;
grant all on MOBILE_DEVICE to group read_write;
grant all on MOBILE_DEVICE to group schema_manager;

GRANT USAGE, SELECT ON SEQUENCE mobile_device_id_seq TO READ_ONLY;
GRANT USAGE, SELECT ON SEQUENCE mobile_device_id_seq TO group read_write;
GRANT USAGE, SELECT ON SEQUENCE mobile_device_id_seq TO group schema_manager;

--

CREATE TABLE MOBILE_DEVICE_HISTORY(
  ID bigint NOT NULL REFERENCES mobile_device (id),
  EVENT_TS timestamp default now(),
  EVENT VARCHAR(255),
  DETAIL varchar
);

grant select on MOBILE_DEVICE_HISTORY to group read_only;
grant all on MOBILE_DEVICE_HISTORY to group read_write;
grant all on MOBILE_DEVICE_HISTORY to group schema_manager;

-- migrate existing data

INSERT INTO mobile_device (player_id, game_type, platform, app_id, device_id, push_token, active)
  SELECT player_id, game_type, 'ANDROID', NULL, NULL, registration_id, true FROM gcm_player_device;

INSERT INTO mobile_device (player_id, game_type, platform, app_id, device_id, push_token, active)
  SELECT player_id, game_type, 'IOS', bundle, NULL, device_token, true FROM ios_player_device;

INSERT INTO mobile_device (player_id, game_type, platform, app_id, device_id, push_token, active)
  SELECT player_id, game_type, 'AMAZON', NULL, NULL, registration_id, active FROM messaging_player_device_registration;
