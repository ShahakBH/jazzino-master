CREATE TABLE IF NOT EXISTS CRM_PLAYER_STATUS (
  PLAYER_ID bigint(20) NOT NULL,
  REGISTERED tinyint(1) NOT NULL,
  VERIFIED tinyint(1) NOT NULL,
  PRIMARY KEY(PLAYER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#