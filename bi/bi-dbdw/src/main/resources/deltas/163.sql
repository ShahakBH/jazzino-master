-- this is done using stored procedures because there not all of the tables exist on internal environment so the SP checks for existance before updating.

DROP PROCEDURE IF EXISTS update_session_platform_column_quietly_on_table#
DROP PROCEDURE IF EXISTS update_platform_column_quietly_on_table#
DROP PROCEDURE IF EXISTS update_registration_platform_column_quietly_on_table#

CREATE PROCEDURE update_session_platform_column_quietly_on_table(IN tableName varchar(64))
BEGIN

    if (select count(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name = tableName) > 0 then
      SET @qry = CONCAT('update ', tableName, ' set session_platform = "IOS" where session_platform = "MOBILE"');
      PREPARE stmt1 FROM @qry;
      EXECUTE stmt1;
      DEALLOCATE PREPARE stmt1;
    end if;

END#

CREATE PROCEDURE update_platform_column_quietly_on_table(IN tableName varchar(64))
BEGIN

    if (select count(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name = tableName) > 0 then
      SET @qry = CONCAT('update ', tableName, ' set platform = "IOS" where platform = "MOBILE"');
      PREPARE stmt1 FROM @qry;
      EXECUTE stmt1;
      DEALLOCATE PREPARE stmt1;
    end if;

END#

CREATE PROCEDURE update_registration_platform_column_quietly_on_table(IN tableName varchar(64))
BEGIN

    if (select count(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name = tableName) > 0 then
      SET @qry = CONCAT('update ', tableName, ' set registration_platform = "IOS" where registration_platform = "MOBILE"');
      PREPARE stmt1 FROM @qry;
      EXECUTE stmt1;
      DEALLOCATE PREPARE stmt1;
    end if;

END#

-- approx. number of rows as of 22/08 in prod
call update_platform_column_quietly_on_table('ACCOUNT_SESSION')# -- 2.5M
call update_registration_platform_column_quietly_on_table('LOBBY_USER')# -- 650K
call update_platform_column_quietly_on_table('OPERATIONS_PLATFORM')# -- 20
call update_registration_platform_column_quietly_on_table('PLAYER_ACCOUNT_INFO')# -- 670K
call update_session_platform_column_quietly_on_table('PLAYER_CLOSED_SESSION')# -- 5.5M
call update_registration_platform_column_quietly_on_table('PLAYER_INFO')# -- 670K
call update_session_platform_column_quietly_on_table('PLAYER_STATUS_CURRENT')# -- 11K
call update_session_platform_column_quietly_on_table('PLAYER_STATUS_CURRENT_X')# -- 0

call update_platform_column_quietly_on_table('registration_by_date_platform_game_type_source')# -- 250

call update_platform_column_quietly_on_table('rpt_activity_by_account_id')# -- 3.5M
call update_platform_column_quietly_on_table('rpt_activity_by_account_id_monthly')# -- 700K
call update_platform_column_quietly_on_table('rpt_activity_by_account_id_weekly')# -- 1.3M
call update_platform_column_quietly_on_table('rpt_players_by_platform_and_day')# -- 22K
call update_platform_column_quietly_on_table('rpt_players_by_platform_and_hour')# -- 41K
call update_platform_column_quietly_on_table('rpt_players_by_platform_and_time')# -- 25M ~6min
call update_platform_column_quietly_on_table('rpt_players_by_platform_and_week')# -- 22K
call update_platform_column_quietly_on_table('rpt_recent_registrations')# -- 10K


DROP PROCEDURE IF EXISTS update_session_platform_column_quietly_on_table#
DROP PROCEDURE IF EXISTS update_platform_column_quietly_on_table#
DROP PROCEDURE IF EXISTS update_registration_platform_column_quietly_on_table#
