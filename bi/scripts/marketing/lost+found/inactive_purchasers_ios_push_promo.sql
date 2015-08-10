mysql -uroot

use damjan;

drop function if exists damjan.mxm_csv;

create function damjan.mxm_csv(rpx_provider varchar(255), user_id bigint, external_id varchar(255))
returns varchar(255) deterministic
return concat(
    '"',
    rpx_provider,
    '"|"',
    if(rpx_provider = 'YAZINO', user_id, external_id),
    '"'
);

drop table if exists damjan.ios_purchasers_1;

create table damjan.ios_purchasers_1 as
select distinct
    p.player_id,
    et.account_id,
    balance
from strataproddw.EXTERNAL_TRANSACTION et, strataprod.PLAYER p, strataprod.ACCOUNT a
where et.account_id = p.account_id
and et.account_id = a.account_id
and external_transaction_status = 'success'
and cashier_name = 'itunes'
group by 1, 2, 3;

alter table damjan.ios_purchasers_1
add primary key (player_id);

drop table if exists damjan.ios_purchasers_2;

create table damjan.ios_purchasers_2 as
select
    p.player_id,
    p.account_id,
    p.balance,
    substring_index(group_concat(distinct game_type order by audit_date desc), ',', 1) last_game_played
from damjan.ios_purchasers_1 p, strataproddw.rpt_account_activity aa
where p.player_id = aa.player_id
and game_type in ('BLACKJACK', 'SLOTS', 'HIGH_STAKES')
group by 1, 2, 3
having max(audit_date) < curdate() - interval 7 day;

alter table damjan.ios_purchasers_2
add primary key (player_id);

drop table if exists damjan.ios_purchasers_export;

create table damjan.ios_purchasers_export as
select
    p.player_id,
    p.balance,
    game_type
from damjan.ios_purchasers_2 p, strataprod.IOS_PLAYER_DEVICE d
where p.player_id = d.player_id
and last_game_played = game_type;

drop table if exists damjan.ios_purchasers_mxm;

create table damjan.ios_purchasers_mxm as
select 
    damjan.mxm_csv(rpx_provider, user_id, external_id) xxx,
    balance
from damjan.ios_purchasers_2 p, strataproddw.LOBBY_USER lu
where p.player_id = lu.player_id;

exit

mysql -uroot -e "select game_type, player_id from damjan.ios_purchasers_export where balance > 100000" | sed "s/\s/,/g" > ios_high_balance.csv
mysql -uroot -e "select game_type, player_id from damjan.ios_purchasers_export where balance <= 100000" | sed "s/\s/,/g" > ios_low_balance.csv

mysql -uroot -e "select player_id from damjan.ios_purchasers_export where balance > 100000" > promo_high_balance.csv
mysql -uroot -e "select player_id from damjan.ios_purchasers_export where balance <= 100000" > promo_low_balance.csv

mysql -uroot -e "select xxx from damjan.ios_purchasers_mxm where balance > 100000" > mxm_high_balance.csv
mysql -uroot -e "select xxx from damjan.ios_purchasers_mxm where balance <= 100000" > mxm_low_balance.csv

zip /tmp/inactive_purchasers.zip *_balance.csv

mysql -uroot

drop table if exists damjan.ios_purchasers_1;
drop table if exists damjan.ios_purchasers_2;
drop table if exists damjan.ios_purchasers_mxm;
drop table if exists damjan.ios_purchasers_export;

exit
