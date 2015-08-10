usjraetest

Our FB notifications are now unblocked, and I'd like to try testing them again today while we have a 2.5K bonus for all players on. To keep this within the test parameters recommended by Facebook, we can send a max of 50,000 notifications per application in a week. For the targeting, can we please create the following:
1) Active players that have not exceeded 2 days of inactivity (no more than 2 days inactive from today)
2) Deduplicated by game 
3) Max 50K players per app

Details here for whoever can help:
Our FB notifications are now unblocked, and I'd like to try testing them again today while we have a 2.5K bonus for all players on. To keep this within the test parameters recommended by Facebook, we can send a max of 50,000 notifications per application in a week. For the targeting, can we please create the following csvs:
1) Active players (within 14 days) that have not exceeded 2 days of inactivity (no more than 2 days inactive from today)
2) Deduplicated by game [priority for deduplication: Wheel Deal, High Stakes, Blackjack, Texas Hold'em, Roulette, Bingo, Hissteria]
3) Max 50K players per app
Can you let me know what the player counts by game are for this criteria? If its really small we can adjust the targeting to include more players.




drop table if exists damjan.priorities;

create table damjan.priorities as
select 'SLOTS' game_type, 1 priority union
select 'HIGH_STAKES', 2 union
select 'BLACKJACK', 3 union
select 'TEXAS_HOLDEM', 4 union
select 'ROULETTE', 5 union
select 'BINGO', 6 union
select 'HISSTERIA', 7;


drop table if exists damjan.xxx;

create table damjan.xxx as
select
    aa.player_id,
    external_id,
    substring_index(group_concat(distinct aa.game_type order by priority), ',', 1) game_type,
    count(distinct audit_date) num_days_played
from strataproddw.rpt_account_activity aa, strataproddw.LOBBY_USER lu, damjan.priorities p
where aa.player_id = lu.player_id
and aa.game_type = p.game_type
and audit_date >= curdate() - interval 7 day
and aa.game_type <> ''
and provider_name = 'facebook'
group by 1
having max(audit_date) < curdate() - interval 2 day;

alter table damjan.xxx add primary key (player_id);

drop table if exists damjan.fb_web_players;

create table damjan.fb_web_players as
select
    distinct p.player_id
from strataproddw.ACCOUNT_SESSION ass, strataproddw.LOBBY_USER lu, strataprod.PLAYER p
where ass.account_id = p.account_id
and p.player_id = lu.player_id
and platform in ('FACEBOOK_CANVAS', 'WEB')
and provider_name = 'facebook'
and tsstarted >= curdate() - interval 7 day;

alter table damjan.fb_web_players add primary key (player_id);

delete from damjan.xxx where player_id not in (select player_id from damjan.fb_web_players);

mysql -uroot -e "select game_type, player_id, external_id from damjan.xxx" | sed "s/\s/,/g" > fb_2_7.csv

select
    game_type,
    count(1)
from damjan.xxx a
group by 1;










game_type, player_id, external_id


select
    a.game_type,
    num_days_played,
    count(1) num_players,
    total_num_players,
    round(100 * count(1) / total_num_players, 2) pct_players
from damjan.yyy a, (select game_type, count(1) total_num_players from damjan.yyy group by 1) b
where a.game_type = b.game_type
group by 1, 2;



select
    a.game_type,
    num_days_played,
    count(1) num_players,
    num_players total_num_players,
    round(100 * count(1) / num_players, 2) pct_players    
from damjan.xxx a, (select game_type, count(1) num_players from damjan.xxx group by 1) b
where a.game_type = b.game_type
group by 1, 2;

select
    game_type,
    count(1) num_players
from damjan.xxx
group by 1;





create table damjan.yyy as
select
    aa.player_id,
    substring_index(group_concat(distinct aa.game_type order by priority), ',', 1) game_type,
    count(distinct audit_date) num_days_played
from strataproddw.rpt_account_activity aa, strataproddw.LOBBY_USER lu, damjan.priorities p
where aa.player_id = lu.player_id
and aa.game_type = p.game_type
and audit_date >= curdate() - interval 14 day
and aa.game_type <> ''
and provider_name = 'facebook'
group by 1;




