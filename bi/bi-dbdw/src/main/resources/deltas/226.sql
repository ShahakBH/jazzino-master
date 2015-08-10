CREATE TABLE FACEBOOK_EXCLUSIONS (
    player_id int(11) NOT NULL,
  	game_type varchar(255) NOT NULL,
    reason varchar(32),
    PRIMARY KEY (player_id,game_type)
)#
