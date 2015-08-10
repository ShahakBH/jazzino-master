drop table if exists activity_segments;

CREATE TABLE activity_segments (
  player_id bigint(20) NOT NULL,
  segment varchar(48) DEFAULT NULL,
  display_name varchar(255) DEFAULT NULL,
  balance decimal(64,4) NOT NULL,
  last_game_played varchar(32) DEFAULT NULL,
  external_id varchar(255) DEFAULT NULL,
  email_address varchar(255) DEFAULT NULL,
  email_opt_in tinyint(1) DEFAULT NULL,
  brite_verify_status varchar(1) DEFAULT NULL,
  provider_name varchar(255) DEFAULT NULL,
  registration_platform varchar(255) DEFAULT NULL
);

drop procedure if exists activity_segments;

delimiter #

create procedure activity_segments()
begin
    drop table if exists tmp_activity_segments;

    create temporary table tmp_activity_segments(
        player_id bigint(20) not null,
        segment varchar(48) not null,
        primary key (player_id, segment)
    );

    insert into tmp_activity_segments
    select
        player_id,
        'Registered yesterday' segment
    from strataprod.PLAYER
    where tscreated >= curdate() - interval 1 day
    and tscreated < curdate();

    insert into tmp_activity_segments
    select distinct
        ss.player_id,
        concat('campaign - ', cd.name) segment
    from strataproddw.CAMPAIGN_DEFINITION cd inner join strataproddw.CAMPAIGN_RUN cr
    on cd.id = cr.campaign_id
    inner join strataproddw.SEGMENT_SELECTION ss
    on cr.id = ss.campaign_run_id
    where cd.id in (58, 59, 60, 61, 74, 75, 76, 77)
    and run_ts >= curdate();

    insert into tmp_activity_segments
    select
        pps.player_id,
        if (
            datediff(curdate(),  date(last_topup_date)) = 1,
            'Last login yesterday',
            if (
                datediff(curdate(), date(last_topup_date)) = 2,
                concat('Last login 2 days ago ', if(p.player_id is null, 'np', 'p')),
                if (
                    datediff(curdate(), date(last_topup_date)) = 4,
                    concat('Last login 4 days ago ', if(p.player_id is null, 'np', 'p')),
                    if (
                        datediff(curdate(), date(last_topup_date)) = 7,
                        concat('Last login 7 days ago ', if(p.player_id is null, 'np', 'p')),
                        if (
                            datediff(curdate(), date(last_topup_date)) = 14,
                            concat('Last login 14 days ago ', if(p.player_id is null, 'np', 'p')),
                            if (
                                datediff(curdate(), date(last_topup_date)) = 28,
                                concat('Last login 28 days ago ', if(p.player_id is null, 'np', 'p')),
                                'other'
                            )
                        )
                    )
                )
            )
        ) segment
    from strataprod.PLAYER_PROMOTION_STATUS pps left join bi.purchase p
    on pps.player_id = p.player_id
    where last_topup_date >= curdate() - interval 28 day
    group by 1, 2;

    delete from tmp_activity_segments
    where segment = 'other';

    delete tmp_activity_segments
    from tmp_activity_segments
    inner join strataprod.PLAYER p
    on tmp_activity_segments.player_id = p.player_id
    where segment = 'Last login yesterday'
    and tscreated >= curdate() - interval 3 day;

    insert into tmp_activity_segments
    select
        player_id,
        'Last login 7 - 14 days ago' segment
    from strataprod.PLAYER_PROMOTION_STATUS
    where last_topup_date >= curdate() - interval 7 day
    and last_topup_date <= curdate() - interval 14 day;

    insert into tmp_activity_segments
    select
        pps.player_id,
        if(
            count(p.player_id) = 0,
            'Last login in last 28 days - non buyer',
            'Last login in last 28 days - buyer'
        ) segment
    from strataprod.PLAYER_PROMOTION_STATUS pps left join bi.purchase p
    on pps.player_id = p.player_id
    where last_topup_date >= curdate() - interval 28 day
    group by 1;

    insert into tmp_activity_segments
    select
        lp.player_id,
        'Buyer inactive 28 days'
    from bi.purchase p, strataproddw.LAST_PLAYED lp
    where p.player_id = lp.player_id
    group by 1
    having max(last_played) < curdate() - interval 28 day;

    insert into tmp_activity_segments
    select
        player_id,
        'Last login 28 - 120 days ago'
    from strataprod.PLAYER_PROMOTION_STATUS
    where last_topup_date <= curdate() - interval 28 day
    and last_topup_date >= curdate() - interval 120 day;

    insert into tmp_activity_segments
    select
        player_id,
        'Last login 121 - 240 days ago'
    from strataprod.PLAYER_PROMOTION_STATUS
    where last_topup_date <= curdate() - interval 121 day
    and last_topup_date >= curdate() - interval 240 day;

    insert into tmp_activity_segments
    select
    lp.player_id,
    'Last played Wheel Deal 0-90 days ago minus reg last 3d'
    from strataproddw.LAST_PLAYED lp, strataprod.PLAYER p
    where lp.player_id = p.player_id
    and tscreated < curdate() - interval 3 day
    and game_type = 'SLOTS'
    and last_played > curdate() - interval 90 day;

    truncate activity_segments;

    insert into activity_segments
      select
        t.player_id,
        segment,
        display_name,
        balance,
        null,
        external_id,
        lu.email_address,
        email_opt_in,
        ev.status,
        provider_name,
        null
      from tmp_activity_segments t
        inner join strataprod.PLAYER p
          on t.player_id = p.player_id
        inner join strataprod.ACCOUNT a
          on p.account_id = a.account_id
        inner join strataprod.LOBBY_USER lu
          on t.player_id = lu.player_id
        left join strataprod.EMAIL_VALIDATION ev
          on lu.email_address = ev.email_address;

    update activity_segments ass, strataproddw.PLAYER_REFERRER pr
    set ass.registration_platform = pr.registration_platform
    where ass.player_id = pr.player_id;

    drop table if exists tmp_activity_segments_last_game_played;

    create temporary table tmp_activity_segments_last_game_played
    select
        ass.player_id,
        substring_index(group_concat(game_type order by last_played desc), ',', 1) last_game_played
    from activity_segments ass, strataproddw.LAST_PLAYED lp
    where ass.player_id = lp.player_id
    group by 1;

    alter table tmp_activity_segments_last_game_played
    add primary key (player_id);

    update activity_segments ass, tmp_activity_segments_last_game_played gp
    set ass.last_game_played = gp.last_game_played
    where ass.player_id = gp.player_id;
end#

delimiter ;
