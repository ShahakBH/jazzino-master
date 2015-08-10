SET FOREIGN_KEY_CHECKS = 0
#

TRUNCATE TABLE TROPHY
#

INSERT INTO TROPHY (TROPHY_ID, TROPHY_IMAGE, GAME_TYPE, TROPHY_NAME, MESSAGE, SHORT_DESCRIPTION, MESSAGE_CABINET)
VALUES
	(69070,'medal_1_BLACKJACK','BLACKJACK','medal_1','%1$s received a medal for placing 1st in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 1st in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.'),
	(69071,'medal_2_BLACKJACK','BLACKJACK','medal_2','%1$s received a medal for placing 2nd in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 2nd in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.'),
	(69072,'medal_3_BLACKJACK','BLACKJACK','medal_3','%1$s received a medal for placing 3rd in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 3rd in Jack\'s Table Blackjack competitions. Challenge them in a competition at Yazino.'),
	(69073,'medal_1_TEXAS_HOLDEM','TEXAS_HOLDEM','medal_1','%1$s received a medal for placing 1st in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 1st in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.'),
	(69074,'medal_2_TEXAS_HOLDEM','TEXAS_HOLDEM','medal_2','%1$s received a medal for placing 2nd in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 2nd in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.'),
	(69075,'medal_3_TEXAS_HOLDEM','TEXAS_HOLDEM','medal_3','%1$s received a medal for placing 3rd in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.',NULL,'%1$s has %2$s medals for placing 3rd in The Round Up Texas Hold\'em competition. Challenge them in a competition at Yazino.'),
	(69076,'trophy_weeklyChamp_TEXAS_HOLDEM','TEXAS_HOLDEM','trophy_weeklyChamp','%1$s is the weekly Round Up Champ! Challenge them to a game of Texas Hold\'em at Yazino.','You are the weekly Round Up Champ!','%1$s has %2$s weekly Round Up Champ trophies! Challenge them to a game of Texas Hold\'em at Yazino.'),
	(69077,'trophy_weeklyChamp_BLACKJACK','BLACKJACK','trophy_weeklyChamp','%1$s is the weekly Jack\'s Table Champ! Challenge them to a game of Blackjack at Yazino.','You are the weekly Jack\'s Table Champ!','%1$s has %2$s weekly Jack\'s Table Champ trophies! Challenge them to a game of Blackjack at Yazino.')
#

SET FOREIGN_KEY_CHECKS = 1
#
