-- WEB-3807 - Remove AGL Blackjack IOS_PLAYER_DEVICE entries

DELETE FROM IOS_PLAYER_DEVICE where GAME_TYPE="BLACKJACK" and BUNDLE="com.yazino.Blackjack"#
