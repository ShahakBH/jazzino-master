drop event if exists evt_set_user_country_from_geoloc#

create event evt_set_user_country_from_geoloc
on schedule every 1 minute
starts curdate()
do update strataproddw.LOBBY_USER_INFO lu, strataprod.PLAYER p, strataproddw.GEOLOC_ACCOUNTS ga, strataproddw.GEOLOC_LOCATIONS gl
set lu.country = gl.country
where lu.player_id = p.player_id
and p.account_id = ga.account_id
and ga.location_id = gl.location_id
and lu.country is null
and tscreated >= now() - interval 1 hour#
