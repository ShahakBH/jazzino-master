drop procedure if exists fillRegistrationByDateSourceAndPlatform#

create procedure fillRegistrationByDateSourceAndPlatform()
begin
    replace into rpt_registrations_by_date_source_and_platform(registration_date, registration_platform, source, users)
    select
        date(p.tscreated),
        if(
            l.registration_platform = 'IOS',
            'iOS',
            if(
                l.registration_platform = 'ANDROID',
                'Android',
                'Web'
            )
        ),
        ifnull(if(s.source = '', 'natural', s.source), 'natural'),
        count(distinct p.account_id)
    from strataproddw.rpt_account_sources_mv s
    join strataproddw.PLAYER_DEFINITION p using (account_id)
    join strataproddw.LOBBY_USER l using (player_id)
    where p.tscreated is not null
    and p.tscreated >= curdate() - interval 1 day
    group by 1, 2, 3;
end#

drop event if exists evtFillRegistrationByDateSourceAndPlatform#

create event evtFillRegistrationByDateSourceAndPlatform
on schedule every 10 minute
comment 'fill the rpt_player_sources_mv materialized view'
do call fillregistrationbydatesourceandplatform()#
