DROP VIEW IF EXISTS bi_verification_status_per_10_mins#
DROP VIEW IF EXISTS bi_verification_status_per_hour#
DROP VIEW IF EXISTS bi_verification_status_per_day#

DROP TABLE IF EXISTS bi_verification_status#
 CREATE TABLE bi_verification_status(
    date date not null,
    rpx varchar(32) not null,
    played_verified int,
    played_not_verified int,
    not_played_verified int,
    not_played_not_verified int,
    primary key(date, rpx)
)#


drop procedure if exists bi_verification_status#

create procedure bi_verification_status()
begin
  REPLACE INTO bi_verification_status(
     date,
     rpx,
     played_verified,
     played_not_verified,
     not_played_verified,
     not_played_not_verified )
    SELECT
        date(rr.AUDIT_TIME) AS TIME_PERIOD,
        rr.RPX as RPX,
        SUM(
            IF ( STRCMP( rr.RPX , 'YAZINO' )  OR  STRCMP(rr.RPX , 'facebook') OR STRCMP(rr.RPX , 'FACEBOOK') ,
                IF(rr.PLAYED>0,
                    IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0),
                    0
                ),
                0
            )
        ) AS  PLAYED_VERIFIED,
        SUM(
            IF ( STRCMP( rr.RPX , 'YAZINO' )  OR  STRCMP(rr.RPX , 'facebook') OR STRCMP(rr.RPX , 'FACEBOOK') ,
                IF(rr.PLAYED>0,
                    IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1),
                    0
                ),
                0
            )
        ) AS PLAYED_NOT_VERIFIED,
        SUM(
            IF ( STRCMP( rr.RPX , 'YAZINO' )  OR  STRCMP(rr.RPX , 'facebook') OR STRCMP(rr.RPX , 'FACEBOOK') ,
                IF(rr.PLAYED>0,
                    0,
                    IF(lu.VERIFICATION_IDENTIFIER IS NULL,1,0)
                ),
                0
            )
        ) AS  NOT_PLAYED_VERIFIED,
        SUM(
            IF ( STRCMP( rr.RPX , 'YAZINO' )  OR  STRCMP(rr.RPX , 'facebook') OR STRCMP(rr.RPX , 'FACEBOOK') ,
                IF(rr.PLAYED>0,
                    0,
                    IF(lu.VERIFICATION_IDENTIFIER IS NULL,0,1)
                ),
                0
            )
        ) AS  NOT_PLAYED_NOT_VERIFIED

        FROM rpt_recent_registrations rr
        JOIN PLAYER_DEFINITION pd USING (ACCOUNT_ID)
        JOIN LOBBY_USER lu USING (USER_ID)
        WHERE rr.RPX != 'google'
        GROUP BY date(rr.AUDIT_TIME) , rr.RPX
        ORDER BY rr.AUDIT_TIME DESC;
end#

drop event if exists  evt_bi_verification_status#

create event evt_bi_verification_status
on schedule every 1 day
starts curdate() + interval 1 day + interval 10 hour
do call bi_verification_status()#

