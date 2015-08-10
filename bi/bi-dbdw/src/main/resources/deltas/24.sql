-- improve performance of mailstats queries

--
-- UNIQUE_PLAYERS_YESTERDAY was taking ~4000 secs this runs in ~20 secs
--
-- create or replace view UNIQUE_PLAYERS_YESTERDAY as
-- select distinct a.player_id, p.partner_id
-- from rpt_account_activity a join strataprod.PLAYER p on a.player_id = p.player_id
-- where audit_date = current_date - interval 1 day and game_type != ''#

--
-- GAME_STATS_YESTERDAY was taking ~2000 secs this runs in around 20 secs
--
create or replace view GAMES_YESTERDAY as
select count(distinct a.table_id, a.game_id) as games, t.game_type
from AUDIT_CLOSED_GAME_PLAYER a join strataprod.TABLE_INFO t on a.table_id = t.table_id
where a.audit_ts >= current_date - interval 1 day and a.audit_ts < current_date
group by t.game_type#

create or replace view PLAYERS_YESTERDAY as
select players, game_type from rpt_players_daily
where reportDate = current_date - interval 1 day
  and game_type != ''#

create or replace view GAME_STATS_YESTERDAY as
select games, players, p.game_type from PLAYERS_YESTERDAY p left join GAMES_YESTERDAY g on p.game_type = g.game_type#

