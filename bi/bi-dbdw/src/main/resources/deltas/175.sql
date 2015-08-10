DROP TRIGGER IF EXISTS players_by_platform_and_time_trigger#
CREATE TRIGGER players_by_platform_and_time_trigger
AFTER INSERT ON rpt_players_by_platform_and_time
FOR EACH ROW
BEGIN
  DECLARE source_val varchar(255) DEFAULT '';

  SELECT s.SOURCE into source_val
		FROM rpt_account_sources_mv s
		WHERE s.ACCOUNT_ID = NEW.ACCOUNT_ID;

  INSERT INTO rpt_players_by_platform_and_time_count(PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM,PLAYERS,SOURCE)
  	VALUES(NEW.PERIOD,
  	    NEW.AUDIT_DATE,
  	    NEW.GAME_TYPE,
  		NEW.PLATFORM,
  		1,
  		IFNULL(source_val,'Natural'))
  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;

  INSERT INTO rpt_players_by_platform_and_time_count_all_sources(PERIOD,AUDIT_DATE,GAME_TYPE,PLATFORM,PLAYERS)
  	VALUES(NEW.PERIOD,
  		IF(NEW.PERIOD = 'mon',NEW.AUDIT_DATE - INTERVAL DAYOFMONTH(NEW.AUDIT_DATE) - 1 DAY,
		IF(NEW.PERIOD = 'week',NEW.AUDIT_DATE - INTERVAL WEEKDAY(NEW.AUDIT_DATE) day, NEW.AUDIT_DATE)),
  		NEW.GAME_TYPE,
  		NEW.PLATFORM,
        1)
  	ON DUPLICATE KEY
	  	UPDATE PLAYERS = PLAYERS+1;
END
#

-- nikit special, table in production and is used by reporting
CREATE TABLE if not exists rpt_registrations_by_date_source_and_platform (
  REGISTRATION_DATE date NOT NULL,
  REGISTRATION_PLATFORM varchar(7) CHARACTER SET utf8 NOT NULL DEFAULT '',
  SOURCE varchar(255) NOT NULL DEFAULT '',
  USERS bigint(21) NOT NULL DEFAULT '0',
  PRIMARY KEY (REGISTRATION_DATE,REGISTRATION_PLATFORM,SOURCE)
) ENGINE=InnoDB DEFAULT CHARSET=latin1#

drop procedure if exists fillRegistrationByDateSourceAndPlatform#

create procedure fillRegistrationByDateSourceAndPlatform()
begin
    replace into rpt_registrations_by_date_source_and_platform(registration_date, registration_platform, source, users)
    select
        date(p.tscreated),
        l.registration_platform,
        ifnull(if(s.source = '', 'natural', s.source), 'natural'),
        count(distinct p.account_id)
    from strataproddw.rpt_account_sources_mv s
    join strataproddw.PLAYER_DEFINITION p using (account_id)
    join strataproddw.LOBBY_USER l using (player_id)
    where p.tscreated is not null
    and p.tscreated >= curdate() - interval 1 day
    group by 1, 2, 3;
end#

