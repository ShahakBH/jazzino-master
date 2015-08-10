insert into CAMPAIGN_DEFINITION values(-1337,'GIFTING','SELECT receiver_id as player_id, l.display_name as sender_name from gifts_sent
 join lobby_user as l on l.player_id=gifts_sent.sender_id
 left join gifts_collected c on gifts_sent.gift_id = c.gift_id
 where (select max(run_ts) from campaign_run_audit where campaign_id=-1337) -interval \'1 minutes\' < gifts_sent.sent_ts
 and getDate() -interval \'1 minutes\' > gifts_sent.sent_ts
 and c.gift_id is null',null,true)#

insert into CAMPAIGN_SCHEDULE values(-1337, current_timestamp, 0,'2023-01-01', 5)#

insert into CAMPAIGN_CHANNEL values(-1337, (select id from CHANNEL_TYPE where CHANNEL_NAME='FACEBOOK_APP_TO_USER_REQUEST'))#
insert into CAMPAIGN_CHANNEL values(-1337, (select id from CHANNEL_TYPE where CHANNEL_NAME='IOS'))#
insert into CAMPAIGN_CHANNEL values(-1337, (select id from CHANNEL_TYPE where CHANNEL_NAME='GOOGLE_CLOUD_MESSAGING_FOR_ANDROID'))#
insert into CAMPAIGN_CHANNEL values(-1337, (select id from CHANNEL_TYPE where CHANNEL_NAME='EMAIL'))#

insert into CAMPAIGN_CONTENT values(-1337, 'description','{SENDER_NAME} sent you a free chip gift. Collect and play now!')#
insert into CAMPAIGN_CONTENT values(-1337, 'message','{SENDER_NAME} sent you a free chip gift. Collect and play now!')#
insert into CAMPAIGN_CONTENT values(-1337, 'tracking','gift_received')#
