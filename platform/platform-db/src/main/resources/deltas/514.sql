create table if not exists LOCKOUT_BONUS(
      PLAYER_ID decimal(16,2) NOT NULL ,
      LAST_BONUS timestamp NULL,
     PRIMARY KEY (PLAYER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#
