#!/bin/bash
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table if exists priorities;

create table priorities as
select 'SLOTS' game_type, 1 priority union
select 'HIGH_STAKES', 2 union
select 'BLACKJACK', 3 union
select 'TEXAS_HOLDEM', 4 union
select 'ROULETTE', 5 union
select 'BINGO', 6 union
select 'HISSTERIA', 7;

drop table if exists xxx;

create table xxx as
select
    aa.player_id,
    external_id,
    substring_index(group_concat(distinct aa.game_type order by priority), ',', 1) game_type,
    count(distinct audit_date) num_days_played
from strataproddw.rpt_account_activity aa, strataproddw.LOBBY_USER lu, priorities p
where aa.player_id = lu.player_id
and aa.game_type = p.game_type
and audit_date >= curdate() - interval 7 day
and aa.game_type <> ''
and provider_name = 'facebook'
group by 1
having max(audit_date) < curdate() - interval 2 day;

alter table xxx add primary key (player_id);

drop table if exists fb_web_players;

create table fb_web_players as
select
    distinct p.player_id
from strataproddw.ACCOUNT_SESSION ass, strataproddw.LOBBY_USER lu, strataprod.PLAYER p
where ass.account_id = p.account_id
and p.player_id = lu.player_id
and platform in ('FACEBOOK_CANVAS', 'WEB')
and provider_name = 'facebook'
and tsstarted >= curdate() - interval 7 day;

alter table fb_web_players add primary key (player_id);

delete from xxx where player_id not in (select player_id from fb_web_players);

EOF

rm /tmp/facebook_notifications.zip
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select game_type, player_id, external_id from xxx" | sed "s/[[:space:]]/,/g" > fb_2_7.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select game_type, count(1) from xxx a group by 1" | sed "s/[[:space:]]/,/g" > counts.csv
zip /tmp/facebook_notifications.zip fb_2_7.csv counts.csv

echo "Kind regards, BI Team" | mutt -s "Facebook Notifications" -a /tmp/facebook_notifications.zip -- rhoberman@yazino.com





