-- Table: LOBBY_USER
CREATE TABLE LOBBY_USER (
  PLAYER_ID DECIMAL(16,2) NOT NULL DISTKEY PRIMARY KEY,
  USER_ID DECIMAL(16,2) NOT NULL,
  REG_TS timestamp without time zone SORTKEY,
  DISPLAY_NAME character varying(255),
  REAL_NAME character varying(255),
  FIRST_NAME character varying(255),
  PICTURE_LOCATION character varying(255),
  EMAIL_ADDRESS character varying(255),
  COUNTRY character varying(3),
  EXTERNAL_ID character varying(255),
  PROVIDER_NAME character varying(255) NOT NULL,
  RPX_PROVIDER character varying(255) NOT NULL,
  BLOCKED boolean DEFAULT false NOT NULL,
  DATE_OF_BIRTH date,
  GENDER character varying(1),
  REFERRAL_ID character varying(255),
  VERIFICATION_IDENTIFIER character varying(36)
)
;
GRANT SELECT ON LOBBY_USER TO GROUP READ_ONLY;
GRANT ALL ON LOBBY_USER TO GROUP READ_WRITE;
GRANT ALL ON LOBBY_USER TO GROUP SCHEMA_MANAGER;
