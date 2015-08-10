#!/bin/bash
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table if exists promo;

create table promo as
select
    game_type,
    player_id,
    datediff(curdate(), max(audit_date)) last_played_days_ago,
    if(
        datediff(curdate(), max(audit_date)) <= 13,
        '7-13',
        if (
            datediff(curdate(), max(audit_date)) <= 20,
            '14-20',
            '21-30'
        )
    ) grp
from strataproddw.rpt_account_activity
where audit_date >= curdate() - interval 30 day
and game_type <> ''
group by 1, 2
having last_played_days_ago >= 7;

alter table promo add primary key (player_id, game_type);

drop table if exists promo_exclude;

create table promo_exclude as
select distinct
    player_id
from strataproddw.rpt_account_activity
where audit_date > curdate() - interval 7 day;

alter table promo_exclude add primary key (player_id);

delete p
from promo p, promo_exclude pe
where p.player_id = pe.player_id;

drop table if exists fb;

create table fb as
select
    grp,
    mt.game_type,
    mt.player_id,
    lu.external_id
from promo mt, strataprod.LOBBY_USER lu
where mt.player_id = lu.player_id
and provider_name = 'facebook';

drop table if exists ios;

create table ios as
select distinct
    grp,
    mt.game_type,
    mt.player_id
from promo mt, strataprod.IOS_PLAYER_DEVICE d
where mt.player_id = d.player_id
and mt.game_type = d.game_type;

EOF

rm /tmp/active-players-partitioned.zip

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select player_id from promo where grp = '7-13'" > promo_7_13.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id,external_id from fb where grp = '7-13'" | sed "s/\s/,/g" > fb_7_13.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id from ios where grp = '7-13'" | sed "s/\s/,/g"> ios_7_13.csv

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select player_id from promo where grp = '14-20'" > promo_14_20.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id,external_id from fb where grp = '14-20'" | sed "s/\s/,/g" > fb_14_20.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id from ios where grp = '14-20'" | sed "s/\s/,/g"> ios_14_20.csv

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select player_id from promo where grp = '21-30'" > promo_21_30.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id,external_id from fb where grp = '21-30'" | sed "s/\s/,/g" > fb_21_30.csv
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox  -e "select game_type,player_id from ios where grp = '21-30'" | sed "s/\s/,/g"> ios_21_30.csv

zip /tmp/active-players-partitioned.zip *_7_13.csv *_14_20.csv *_21_30.csv

mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table ios;

drop table fb;

drop table promo_exclude;

drop table promo;

EOF

echo "Attachment contains players for bans 7-13, 14-20 and 21-30 for Promotions, iOS and Facebook.  Kind regards, BI Team" | mutt -s "2) Facebook Notifications " -a /tmp/active-players.zip -- rhoberman@yazino.com