drop table if exists registration_by_date_platform_game_type_source#

create table registration_by_date_platform_game_type_source(
    date date not null,
    platform varchar(32) not null,
    game_type varchar(255) not null,
    source varchar(255) not null,
    num_players int not null,
    primary key(date, platform, game_type, source)
)#

drop procedure if exists registration_by_date_platform_game_type_source#

create procedure registration_by_date_platform_game_type_source(in date_from date, in date_to date)
begin
    insert into registration_by_date_platform_game_type_source(date, platform, game_type, source, num_players)    
    select
        date(tscreated) date,
        platform,
        '*' game_type,
        source,
        count(1) num_players
    from strataproddw.rpt_recent_registrations rr, strataproddw.rpt_player_sources ps
    where rr.user_id = ps.user_id
    and tscreated >= date_from
    and tscreated < date_to
    group by date(tscreated), platform, source
    on duplicate key update
    num_players = values(num_players);

    insert into registration_by_date_platform_game_type_source(date, platform, game_type, source, num_players)
    select
        date(tscreated) date,
        platform,
        '*' game_type,
        '*' source,
        count(1) num_players
    from strataproddw.rpt_recent_registrations rr, strataproddw.rpt_player_sources ps
    where rr.user_id = ps.user_id
    and tscreated >= date_from
    and tscreated < date_to
    group by date(tscreated), platform
    on duplicate key update
    num_players = values(num_players);

    insert into registration_by_date_platform_game_type_source(date, platform, game_type, source, num_players)
    select
        date(tscreated) date,
        '*' platform,
        '*' game_type,
        source,
        count(1) num_players
    from strataproddw.rpt_recent_registrations rr, strataproddw.rpt_player_sources ps
    where rr.user_id = ps.user_id
    and tscreated >= date_from
    and tscreated < date_to
    group by date(tscreated), source
    on duplicate key update
    num_players = values(num_players);

    insert into registration_by_date_platform_game_type_source(date, platform, game_type, source, num_players)
    select
        date(tscreated),
        '*' platform,
        '*' game_type,
        '*' source,
        count(1) num_players
    from strataproddw.rpt_recent_registrations rr, strataproddw.rpt_player_sources ps
    where rr.user_id = ps.user_id
    and tscreated >= date_from
    and tscreated < date_to
    group by date(tscreated)
    on duplicate key update
    num_players = values(num_players);
end#

drop event if exists evt_registration_by_date_platform_game_type_source#

create event evt_registration_by_date_platform_game_type_source
on schedule every 1 day
starts curdate() + interval 1 day + interval 9 hour
do call registration_by_date_platform_game_type_source(curdate() - interval 1 day, curdate())#
