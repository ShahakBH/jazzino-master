ssh mem-prd-dbdw01.mem.yazino.com

mysql -uroot

use damjan;

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

drop table if exists ios;

create table ios as
select distinct
    mt.game_type,
    mt.player_id
from tmp_app_request_target mt, strataprod.IOS_PLAYER_DEVICE d
where mt.player_id = d.player_id
and mt.game_type = d.game_type;

exit

rm /tmp/app.zip
mysql -uroot -e "select * from damjan.fb" | sed "s/\s/,/g" > fb_90.csv
mysql -uroot -e "select * from damjan.ios" | sed "s/\s/,/g"> ios_90.csv
zip /tmp/app.zip fb_90.csv ios_90.csv

logout

scp mem-prd-dbdw01.mem.yazino.com:/tmp/app.zip .
