CREATE TABLE if not exists PAYMENT_STATE_ANDROID (
  STATE varchar(10) not null,
  PLAYER_ID bigint(20) not null,
  INTERNAL_TRANSACTION_ID varchar(255) not null,
  GOOGLE_ORDER_NUMBER varchar(255) null,
  GAME_TYPE varchar(255) not null,
  PRODUCT_ID varchar(255) not null,
  PROMO_ID int(11),
  UPDATED_TS datetime not null,
  PRIMARY KEY (PLAYER_ID, INTERNAL_TRANSACTION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#
