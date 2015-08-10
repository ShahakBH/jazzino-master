create table payout (
    date date,
    platform character varying(32),
    game_type character varying(255),
    name character varying(255),
    num_players bigint,
    total_stake numeric,
    total_return numeric,
    total_num_stakes bigint,
    total_num_returns bigint
);

grant select on table payout to read_only;
grant all on table payout to read_write;

create table slots_payout (
    date date,
    platform character varying(32),
    name character varying(255),
    band text,
    num_players bigint,
    total_stake numeric,
    total_return numeric,
    total_num_stakes bigint,
    total_num_returns bigint,
    pct_payout numeric
);

grant select on table slots_payout to read_only;
grant all on table slots_payout to read_write;

create or replace function agg_payout(date, date) returns date as $$
begin
    while $1 < $2
    loop
        drop table if exists tmp_payout_1;

        create temporary table tmp_payout_1 as
        select
           platform,
           game_type,
           game_variation_template_id,
           tl.table_id,
           transaction_type,
           tl.account_id,
           transaction_ts,
           abs(amount) amount
        from transaction_log tl left join account_session ass
        on tl.session_id = ass.session_id
        inner join table_info ti
        on tl.table_id = ti.table_id
        where transaction_type in ('Stake', 'Return')
        and transaction_ts >= $1
        and transaction_ts < $2;

        create index on tmp_payout_1 (account_id, table_id);

        analyze tmp_payout_1;

        drop table if exists tmp_payout_2;

        create temporary table tmp_payout_2 as
        select
           account_id,
           table_id,
           case
              when max(amount) <= 100 then 'Beginner'
              when max(amount) <= 1000 then 'Rookie'
              when max(amount) <= 10000 then 'Shark'
              when max(amount) <= 100000 then 'High Roller'
              when max(amount) <= 1000000 then 'Royalty'
              when max(amount) <= 10000000 then 'Unlock at Level 14'
              else 'XXX'
           end band
        from tmp_payout_1
        where transaction_type = 'Stake'
        group by 1, 2;

        create index on tmp_payout_2 (account_id, table_id);

        analyze tmp_payout_2;

        drop table if exists tmp_payout_3;

        create temporary table tmp_payout_3 as
        select
           platform,
           game_type,
           game_variation_template_id,
           band,
           sum(case when transaction_type = 'Stake' then amount else 0 end) total_stake,
           sum(case when transaction_type = 'Return' then amount else 0 end) total_return,
           sum(case when transaction_type = 'Stake' then 1 else 0 end) total_num_stakes,
           sum(case when transaction_type = 'Return' then 1 else 0 end) total_num_returns,
           count(distinct a.account_id) num_players
        from tmp_payout_1 a inner join tmp_payout_2 b
        on a.account_id = b.account_id
        and a.table_id = b.table_id
        group by 1, 2, 3, 4;

        analyze tmp_payout_3;

        insert into slots_payout
        select
           $1,
           platform,
           name,
           band,
           num_players,
           total_stake,
           total_return,
           total_num_stakes,
           total_num_returns,
           round(100 * total_return / total_stake, 2) pct_payout
        from tmp_payout_3 a, game_variation_template b
        where a.game_variation_template_id = b.game_variation_template_id
        and a.game_type = 'SLOTS';

        drop table if exists tmp_payout_4;

        create temporary table tmp_payout_4 as
        select
           platform,
           game_type,
           game_variation_template_id,
           sum(case when transaction_type = 'Stake' then amount else 0 end) total_stake,
           sum(case when transaction_type = 'Return' then amount else 0 end) total_return,
           sum(case when transaction_type = 'Stake' then 1 else 0 end) total_num_stakes,
           sum(case when transaction_type = 'Return' then 1 else 0 end) total_num_returns,
           count(distinct account_id) num_players
        from tmp_payout_1
        group by 1, 2, 3;

        insert into payout
        select
           $1,
           platform,
           a.game_type,
           name,
           num_players,
           total_stake,
           total_return,
           total_num_stakes,
           total_num_returns
        from tmp_payout_4 a, game_variation_template b
        where a.game_variation_template_id = b.game_variation_template_id;

        drop table if exists tmp_payout_1;
        drop table if exists tmp_payout_2;
        drop table if exists tmp_payout_3;
        drop table if exists tmp_payout_4;

        $1 := $1 + interval '1 day';
    end loop;

    return $1;
end; $$
LANGUAGE PLPGSQL;
