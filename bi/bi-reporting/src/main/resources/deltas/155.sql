drop materialized view if exists player_purchase_daily_by_game;

create materialized view player_purchase_daily_by_game as
 SELECT et.player_id,
    et.registration_date,
    et.game_type game,
    et.purchase_platform,
    et.purchase_ts::date AS purchase_date,
    sum(et.amount_gbp) AS total_amount_gbp,
    count(1) AS num_purchases
   FROM external_transaction_mv et
  WHERE et.cashier_name::text = ANY (ARRAY['FACEBOOK'::character varying, 'GoogleCheckout'::character varying, 'PayPal'::character varying, 'PayPal-WPP'::character varying, 'Paypal'::character varying, 'iTunes'::character varying, 'PayPal DG'::character varying, 'Wirecard'::character varying, 'Zong'::character varying, 'WorldPay'::character varying, 'Amazon'::character varying]::text[])
  GROUP BY 1, 2, 3, 4, 5;

create unique index on player_purchase_daily_by_game (player_id, purchase_date, purchase_platform, game);

drop table if exists dmr_player_activity_and_purchases_by_game;

create table dmr_player_activity_and_purchases_by_game (
    player_id numeric(16,2) NOT NULL,
    game character varying(255) NOT NULL,
    platform character varying(255) NOT NULL,
    activity_date date NOT NULL,
    referrer character varying(255),
    registration_date date,
    registration_platform character varying(32),
    num_registrations bigint,
    player_purchase_daily_player_id numeric(16,2),
    player_purchase_daily_registration_date date,
    purchase_game character varying(255),
    purchase_platform character varying(32),
    purchase_date date,
    total_amount_gbp numeric,
    num_purchases bigint,
    registration_adnet character varying(32)
);

create unique index on dmr_player_activity_and_purchases_by_game(player_id, game, platform, activity_date);
create index on dmr_player_activity_and_purchases_by_game(activity_date);

grant select on dmr_player_activity_and_purchases_by_game to group read_only;
grant all on dmr_player_activity_and_purchases_by_game to group read_write;
grant all on dmr_player_activity_and_purchases_by_game to group schema_manager;

drop materialized view if exists dmr_player_first_played cascade;

create materialized view dmr_player_first_played as
select distinct on (player_id,game)
    player_id,
    game,
    platform,
    activity_ts::date first_played_date
from public.player_activity_daily
order by player_id, game, activity_ts;

create unique index on dmr_player_first_played (player_id, game);

drop materialized view if exists dmr_player_registration_by_game_platform cascade;

create materialized view dmr_player_registration_by_game_platform as
select distinct on (coalesce(lu.player_id, fp.player_id), coalesce(lu.registration_game_type, fp.game))
    coalesce(lu.player_id, fp.player_id) player_id,
    coalesce(lu.registration_game_type, fp.game) game,
    coalesce(lu.registration_platform, fp.platform) platform,
    coalesce(lu.reg_ts::date, fp.first_played_date) registration_date
from lobby_user lu full outer join dmr_player_first_played fp
on lu.player_id = fp.player_id
and lu.registration_platform = fp.platform
and lu.registration_game_type = fp.game
order by 1, 2, 4;

create unique index on dmr_player_registration_by_game_platform(player_id, game);

drop materialized view if exists dmr_registration_by_game_platform cascade;

create materialized view dmr_registration_by_game_platform as
select game, platform, registration_date, count(1) num_players
from dmr_player_registration_by_game_platform
group by 1, 2, 3;

drop materialized view if exists dmr_registration_by_game_platform_bands cascade;

create materialized view dmr_registration_by_game_platform_bands as
select
    a.registration_date,
    a.game,
    a.platform,
    case
        when a.registration_date - b.registration_date = 0 then '0 days'
        when a.registration_date - b.registration_date = 1 then '1 day'
        when a.registration_date - b.registration_date <= 6 then '1st wk'
        when a.registration_date - b.registration_date <= 27 then '2-4 wks'
        when a.registration_date - b.registration_date <= 364 then '2-12 m'
        else '1+ y'
    end band,
    sum(b.num_players) num_registrations
from dmr_registration_by_game_platform a, dmr_registration_by_game_platform b
where a.game = b.game
and a.platform = b.platform
and a.registration_date >= b.registration_date
group by 1, 2, 3, 4;

GRANT SELECT ON dmr_registration_by_game_platform_bands TO GROUP READ_ONLY;
GRANT ALL ON dmr_registration_by_game_platform_bands TO GROUP READ_WRITE;
GRANT ALL ON dmr_registration_by_game_platform_bands TO GROUP SCHEMA_MANAGER;

create or replace function dmr_by_game(date, date, boolean) returns date as $$
begin
    if $3 then
        refresh materialized view player_purchase_daily_by_game;
        refresh materialized view dmr_player_first_played;
        refresh materialized view dmr_player_registration_by_game_platform;
        refresh materialized view dmr_registration_by_game_platform;
        refresh materialized view dmr_registration_by_game_platform_bands;
    end if;

    while $1 < $2
    loop
        insert into dmr_player_activity_and_purchases_by_game
        select
            pad.player_id,
            pad.game,
            pad.platform,
            pad.activity_ts::date activity_date,
            pad.referrer,
            pad.reg_ts::date registration_date,
            pr.registration_platform,
            r.num_registrations,
            ppd.player_id player_purchase_daily_player_id,
            ppd.registration_date player_purchase_daily_registration_date,
            ppd.game purchase_game,
            ppd.purchase_platform,
            ppd.purchase_date,
            ppd.total_amount_gbp,
            ppd.num_purchases,
            ''
        from player_activity_daily pad inner join player_referrer pr
        on pad.player_id = pr.player_id
        inner join registrations r
        on pad.reg_ts::date = r.registration_date
        and pr.registration_platform = r.registration_platform
        left join player_purchase_daily_by_game ppd
        on pad.player_id = ppd.player_id
        and pad.activity_ts = ppd.purchase_date
        and pad.platform = ppd.purchase_platform
        and pad.game = ppd.game
        where pad.activity_ts >= $1
        and pad.activity_ts < $1 + interval '1 day';

        update dmr_player_activity_and_purchases_by_game
        set registration_adnet = adnet_mappings.registration_adnet
        from adnet_mappings
        where activity_date = $1
        and substring(dmr_player_activity_and_purchases_by_game.referrer, 1, char_length(adnet_mappings.referrer)) = adnet_mappings.referrer;

        $1 := $1 + interval '1 day';
    end loop;

    return $1;
end; $$
LANGUAGE PLPGSQL;
