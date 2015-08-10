#!/bin/bash
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table if exists ios;

create table ios as
select distinct
    game_type,
    player_id
from strataprod.IOS_PLAYER_DEVICE;

drop table if exists android;

create table android as
select distinct
    game_type,
    player_id
from strataproddw.GCM_PLAYER_DEVICE;

EOF

rm /tmp/all_opted_in_mobile_players.zip
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select * from ios" | sed "s/[[:space:]]/,/g" > all_opted_in.ios.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select * from android" | sed "s/[[:space:]]/,/g" > all_opted_in.android.csv
zip /tmp/all_opted_in_mobile_players.zip all_opted_in.ios.csv all_opted_in.android.csv

echo "Kind regards, BI Team" | mutt -s "All opted-in mobile players" -a /tmp/all_opted_in_mobile_players.zip -- aelahmar@yazino.com

