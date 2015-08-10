#!/bin/bash

if [  `echo " " | sed "s/[[:space:]]/,/g"` != "," ]
then
	echo "Unable to continue as this script requires a version of sed that supports the :space: character class."
	exit 1
fi

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table if exists query4whales;
create table query4whales as 
  select player_id 
    from bi.purchase
   where ts_purchase >= curdate() - interval 90 day 
group by player_id 
  having sum(amount_gbp) > 100 and max(ts_purchase) > curdate() - interval 90 day;

alter table query4whales
	add primary key (player_id);

drop table if exists query4inactive;
create table query4inactive as 
      select player_id
        from strataproddw.LAST_PLAYED
       where game_type <> ''
    group by player_id
      having max(last_played) >= curdate() - interval 90 day
         and max(last_played) <= curdate() - interval 7 day;

alter table query4inactive
	add primary key (player_id);

drop table if exists query4inactive_by_game;
create table query4inactive_by_game as 
     select p.player_id, game_type
      from strataproddw.LAST_PLAYED p
inner join query4inactive ia
     where ia.player_id = p.player_id
       and last_played >= curdate() - interval 90 day
       and last_played <= curdate() - interval 7 day
       and game_type <> '';

alter table query4inactive_by_game
	add primary key (player_id, game_type);

drop table if exists query4players_by_game;
create table query4players_by_game as
select ia.game_type, ia.player_id from query4inactive_by_game ia
inner join query4whales w
on ia.player_id = w.player_id;

alter table query4players_by_game
	add primary key (player_id, game_type);

EOF

rm /tmp/inactive_purchasers.zip
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select DISTINCT lu.RPX_PROVIDER,IF(lu.RPX_PROVIDER='YAZINO',lu.USER_ID,lu.EXTERNAL_ID) from query4inactive ia inner join query4whales w on ia.player_id = w.player_id left join strataprod.LOBBY_USER lu on ia.player_id = lu.player_id" | sed "s/[[:space:]]/|/g" | awk -F"|" '{print "\""$1"\"|\"" $2"\""}' > inactive_purchasers.promo.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select distinct p.game_type, p.player_id from query4players_by_game p join strataprod.IOS_PLAYER_DEVICE ios on (ios.player_id = p.player_id AND ios.game_type = p.game_type)  left join strataprod.LOBBY_USER lu on ia.player_id = lu.player_id where (ios.device_token is not NULL)" | sed "s/[[:space:]]/|/g" | awk -F"|" '{print "\""$1"\"|\"" $2"\""}' > inactive_purchasers.ios.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select distinct p.game_type, p.player_id from query4players_by_game p join strataproddw.GCM_PLAYER_DEVICE gcm on (gcm.player_id = p.player_id AND gcm.game_type = p.game_type)   left join strataprod.LOBBY_USER lu on ia.player_id = lu.player_id  where (gcm.registration_id is not null)" | sed "s/[[:space:]]/|/g" | awk -F"|" '{print "\""$1"\"|\"" $2"\""}'> inactive_purchasers.android.csv
zip /tmp/inactive_purchasers.zip inactive_purchasers*.csv

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

#drop table if exists query4whales;
#drop table if exists query4inactive_by_game;
#drop table if exists query4inactive;
#drop table if exists query4players_by_game;

EOF

echo "Kind regards, BI Team" | mutt -s "Inactive Purchasers" -a /tmp/inactive_purchasers.zip -- rhoberman@yazino.com

echo 'done'

