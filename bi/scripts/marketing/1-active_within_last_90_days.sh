#!/bin/bash
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox <<EOF

drop table if exists tmp_app_request_target;

create temporary table tmp_app_request_target as
select distinct
    player_id,
    game_type
from strataproddw.rpt_account_activity
where audit_date >= curdate() - interval 90 day
and game_type <> '';

alter table tmp_app_request_target add primary key (player_id, game_type);

drop table if exists fb;

create table fb as
select
    mt.game_type,
    mt.player_id,
    lu.external_id
from tmp_app_request_target mt, strataprod.LOBBY_USER lu
where mt.player_id = lu.player_id
and provider_name = 'facebook';

EOF

rm /tmp/active_within_last_90_days.zip
mysql -h mem-prd-dbdw01 -u readonly -preadonly-s1gn4tur3 -Dbi-sandbox -e "select * from fb" | sed "s/[[:space:]]/,/g" > inactive_within_last_90_days.fb.csv
zip /tmp/active_within_last_90_days.zip inactive_within_last_90_days.fb.csv 

echo "Kind regards, BI Team" | mutt -s "Players Active within 90 Days" -a /tmp/active_within_last_90_days.zip -- aelahmar@yazino.com

