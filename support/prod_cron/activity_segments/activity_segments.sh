#!/bin/bash

DATE=`date +'%d-%m-%Y'`
DAY=`date +'%A'`

rm -f /tmp/activity_segments*

mysql -uroot -e "call damjan.activity_segments()"
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Registered yesterday'" > /tmp/activity_segments_registered_yesterday.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login yesterday'" > /tmp/activity_segments_last_login_yesterday.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 2 days ago p'" > /tmp/activity_segments_last_login_2_days_ago_p.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 2 days ago np'" > /tmp/activity_segments_last_login_2_days_ago_np.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 4 days ago p'" > /tmp/activity_segments_last_login_4_days_ago_p.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 4 days ago np'" > /tmp/activity_segments_last_login_4_days_ago_np.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 7 days ago p'" > /tmp/activity_segments_last_login_7_days_ago_p.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 7 days ago np'" > /tmp/activity_segments_last_login_7_days_ago_np.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 14 days ago p'" > /tmp/activity_segments_last_login_14_days_ago_p.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 14 days ago np'" > /tmp/activity_segments_last_login_14_days_ago_np.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 28 days ago p'" > /tmp/activity_segments_last_login_28_days_ago_p.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login 28 days ago np'" > /tmp/activity_segments_last_login_28_days_ago_np.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login in last 28 days - non buyer'" > /tmp/activity_segments_last_login_in_last_28_days_non_buyer.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last login in last 28 days - buyer'" > /tmp/activity_segments_last_login_in_last_28_days_buyer.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Buyer inactive 28 days'" > /tmp/activity_segments_buyer_inactive_28_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'Last played Wheel Deal 0-90 days ago minus reg l'" > /tmp/activity_segments_wd_0_90.csv

mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive players - 3 days'" > /tmp/activity_segments_campaign_inactive_players_3_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive Buyer - 3 days'" > /tmp/activity_segments_campaign_inactive_buyer_3_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive players - 6 days'" > /tmp/activity_segments_campaign_inactive_players_6_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive Buyer - 6 days'" > /tmp/activity_segments_campaign_inactive_buyer_6_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive players - 13 days'" > /tmp/activity_segments_campaign_inactive_players_13_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive Buyer - 13 days'" > /tmp/activity_segments_campaign_inactive_buyer_13_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive players - 27 days'" > /tmp/activity_segments_campaign_inactive_players_27_days.csv
mysql -uroot -e "select * from damjan.activity_segments where segment = 'campaign - Inactive Buyer - 27 days'" > /tmp/activity_segments_campaign_inactive_buyer_27_days.csv

zip --quiet /tmp/activity_segments_$DATE.zip /tmp/activity_segments_*.csv

s3cmd put /tmp/activity_segments_$DATE.zip s3://yazino-tmp
s3cmd setacl s3://yazino-tmp/activity_segments_$DATE.zip --acl-public

rm -f /tmp/activity_segments*

mysql -uroot -e "select player_id, display_name, balance, last_game_played, external_id, email_address, email_opt_in, brite_verify_status, provider_name, registration_platform from damjan.activity_segments where segment = 'Last login 28 - 120 days ago' and last_game_played = 'SLOTS' and external_id is not null" > /tmp/activity_segments_last_login_28_120.csv
mysql -uroot -e "select player_id, display_name, balance, last_game_played, external_id, email_address, email_opt_in, brite_verify_status, provider_name, registration_platform from damjan.activity_segments where segment = 'Last login 121 - 240 days ago' and last_game_played = 'SLOTS' and external_id is not null" > /tmp/activity_segments_last_login_121_241.csv

zip --quiet /tmp/activity_segments_28_120_121_240_$DATE.zip /tmp/activity_segments_*.csv

s3cmd put /tmp/activity_segments_28_120_121_240_$DATE.zip s3://yazino-tmp
s3cmd setacl s3://yazino-tmp/activity_segments_28_120_121_240_$DATE.zip --acl-public

echo "Ready on http://yazino-tmp.s3.amazonaws.com/activity_segments_$DATE.zip and http://yazino-tmp.s3.amazonaws.com/activity_segments_28_120_121_240_$DATE.zip" | mailx -s "Activity segments ($DAY, $DATE)" -r no-reply@ovh-prd-dbdw1.yazino.com sansari@yazino.com alyssa@yazino.com aelahmar@yazino.com guillaume@yazino.com dvujnovic@yazino.com isanchez@yazino.com

rm -f /tmp/activity_segments*
