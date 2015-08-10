ssh mem-prd-dbdw01.mem.yazino.com

mysql -uroot

use damjan;

drop table if exists damjan.promo;

create table damjan.promo as
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

alter table damjan.promo add primary key (player_id, game_type);

drop table if exists damjan.promo_exclude;

create table damjan.promo_exclude as
select distinct
    player_id
from strataproddw.rpt_account_activity
where audit_date > curdate() - interval 7 day;

alter table damjan.promo_exclude add primary key (player_id);

delete p
from damjan.promo p, damjan.promo_exclude pe
where p.player_id = pe.player_id;

drop table if exists damjan.fb;

create table damjan.fb as
select
    grp,
    mt.game_type,
    mt.player_id,
    lu.external_id
from damjan.promo mt, strataprod.LOBBY_USER lu
where mt.player_id = lu.player_id
and provider_name = 'facebook';

drop table if exists damjan.ios;

create table damjan.ios as
select distinct
    grp,
    mt.game_type,
    mt.player_id
from damjan.promo mt, strataprod.IOS_PLAYER_DEVICE d
where mt.player_id = d.player_id
and mt.game_type = d.game_type;

exit

rm /tmp/app.zip

mysql -uroot -e "select player_id from damjan.promo where grp = '7-13'" > promo_7_13.csv
mysql -uroot -e "select game_type,player_id,external_id from damjan.fb where grp = '7-13'" | sed "s/\s/,/g" > fb_7_13.csv
mysql -uroot -e "select game_type,player_id from damjan.ios where grp = '7-13'" | sed "s/\s/,/g"> ios_7_13.csv

mysql -uroot -e "select player_id from damjan.promo where grp = '14-20'" > promo_14_20.csv
mysql -uroot -e "select game_type,player_id,external_id from damjan.fb where grp = '14-20'" | sed "s/\s/,/g" > fb_14_20.csv
mysql -uroot -e "select game_type,player_id from damjan.ios where grp = '14-20'" | sed "s/\s/,/g"> ios_14_20.csv

mysql -uroot -e "select player_id from damjan.promo where grp = '21-30'" > promo_21_30.csv
mysql -uroot -e "select game_type,player_id,external_id from damjan.fb where grp = '21-30'" | sed "s/\s/,/g" > fb_21_30.csv
mysql -uroot -e "select game_type,player_id from damjan.ios where grp = '21-30'" | sed "s/\s/,/g"> ios_21_30.csv

zip /tmp/app.zip *_7_13.csv *_14_20.csv *_21_30.csv

mysql -uroot

use damjan;

drop table ios;

drop table fb;

drop table promo_exclude;

drop table promo;

exit

exit

scp mem-prd-dbdw01.mem.yazino.com:/tmp/app.zip .
