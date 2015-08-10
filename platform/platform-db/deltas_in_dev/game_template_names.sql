
-- Add template names for all the other games (blackjack is already configured)

INSERT INTO GAME_CONFIGURATION_PROPERTY (GAME_ID, PROPERTY_NAME, PROPERTY_VALUE)
VALUES
  ('ROULETTE','defaultTemplateName','Roulette'),
  ('TEXAS_HOLDEM','defaultTemplateName','Texas_Holdem'),
  ('SLOTS','defaultTemplateName','Slots'),
  ('HIGH_STAKES','defaultTemplateName','High_Stakes'),
  ('HISSTERIA','defaultTemplateName','Hissteria'),
  ('BINGO','defaultTemplateName','Extreme_Bingo')
#
