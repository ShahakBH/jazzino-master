insert into CAMPAIGN_DEFINITION values(-666,'COUNTDOWN  BONUS','select player_id from lockout_bonus
 where last_bonus_ts >= date_trunc(\'minute\',current_timestamp-interval\'4 HOURS 5 minutes\')
 and last_bonus_ts < date_trunc(\'minute\',current_timestamp-interval\'4 HOURS 0 minute\')
 order by player_id',null,true)#

insert into CAMPAIGN_SCHEDULE values(-666, current_timestamp, 0,'2001-01-01', 5)#

insert into CAMPAIGN_CHANNEL values(-666, (select id from CHANNEL_TYPE where CHANNEL_NAME='FACEBOOK_APP_TO_USER_REQUEST'))#
insert into CAMPAIGN_CHANNEL values(-666, (select id from CHANNEL_TYPE where CHANNEL_NAME='IOS'))#
insert into CAMPAIGN_CHANNEL values(-666, (select id from CHANNEL_TYPE where CHANNEL_NAME='GOOGLE_CLOUD_MESSAGING_FOR_ANDROID'))#

insert into CAMPAIGN_CONTENT values(-666, 'description','2,000 free Chips!')#
insert into CAMPAIGN_CONTENT values(-666, 'message','Your 2,000 FREE chip bonus is ready! Collect it now so it can keep on refilling!')#
insert into CAMPAIGN_CONTENT values(-666, 'tracking','bonus_received')#

insert into CAMPAIGN_CHANNEL_CONFIG values (-666,'GAME_TYPE_FILTER','TEXAS_HOLDEM,BLACKJACK,ROULETTE,SLOTS,HIGH_STAKES')#
