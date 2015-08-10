REPLACE INTO GAME_VARIATION_TEMPLATE_PROPERTY (GAME_VARIATION_TEMPLATE_ID, NAME, VALUE, VERSION)
  SELECT GAME_VARIATION_TEMPLATE_ID, 'SIDE_BETS_ENABLED', 'No', 0 FROM GAME_VARIATION_TEMPLATE where GAME_TYPE = 'BLACKJACK'
#

INSERT IGNORE INTO GAME_VARIATION_TEMPLATE (GAME_VARIATION_TEMPLATE_ID, GAME_TYPE, NAME, VERSION)
VALUES (176, 'BLACKJACK', 'Pharoahs Blackjack Cash', 0)#

REPLACE  INTO GAME_VARIATION_TEMPLATE_PROPERTY (GAME_VARIATION_TEMPLATE_ID, NAME, VALUE, VERSION)
VALUES
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'ALLOW_DOUBLE_DOWN_ON_BLACKJACK', 'No', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'AUTO_BET', 'No', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'AUTO_HIT_PLAYERS_UNTIL', 'Soft 11', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'BET_TIMEOUTS_ALLOWED', '2', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'BLACKJACK_PAYS', '3:2', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'CHIPS', '10,50,100,500,1000,5000,10000,25000,50000,100000', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'DEALER_STANDS_ON', 'Soft 17', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'DEALER_WINS_TIES', 'No', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'DOUBLE_AFTER_SPLIT', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'DOUBLE_ON', 'Any', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'INSURANCE', '2:1', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'JOINING_ALLOWED', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'KILL_TIMEOUT', '120000', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'LEAVING_ALLOWED', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'MAXIMUM_CHIPS_PER_PLAY', '1000000', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'MAXIMUM_NUMBER_OF_SEATS', '5', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'MAX_SEATS_PER_PLAYER', '5', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'MINIMUM_CHIPS_PER_PLAY', '1', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'MINIMUM_NUMBER_OF_SEATS', '1', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'NO_BLACKJACK_ON_SPLITS', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'NUMBER_OF_DECKS', '8', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'NUMBER_OF_HIDDEN_CARDS', '0', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'NUMBER_OF_HOLE_CARDS', '1', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'ONE_CARD_ONLY_AFTER_SPLIT_ACES', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'PEEK_ON', 'A,10,J,Q,K', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'PURGE_TIMEOUT', '500', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SHUFFLE_WHEN_PACK_GOES_BELOW', '60', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SPLIT_ACES_ONCE_ONLY', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SPLIT_HANDS', '2', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SPLIT_UNLIKE_10_VALUE_CARDS', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SURRENDER', 'Late', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'TIMEOUT', '15000', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SUITED_BLACKJACK_PAYS', '15:1', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SIDE_BETS_ENABLED', 'Yes', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'SUITED_BLACKJACK_DEALT_ANIMATION_TIME', '3500', 0),
((select game_variation_template_id from GAME_VARIATION_TEMPLATE where name = 'Pharoahs Blackjack Cash'), 'PER_PLAYER_SUITED_BLACKJACK_WIN_ANIMATION_TIME', '1500', 0)
#
