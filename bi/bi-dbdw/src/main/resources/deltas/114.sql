-- This delta is not needed to recreate the database and will break against current versions of strataprod.

--
-- CREATE TABLE IF NOT EXISTS `TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE` (
--   `player_id` int(11) NOT NULL,
--   `correct_value` tinyint NOT NULL,
--   PRIMARY KEY (`player_id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8#
--
-- INSERT into TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE (player_id, correct_value)
-- SELECT slave.PLAYER_ID, master.blocked from strataproddw.LOBBY_USER slave inner join strataprod.LOBBY_USER master
--   on slave.PLAYER_ID = master.PLAYER_ID
--   where slave.blocked <> master.blocked and master.blocked = 1#
--
-- UPDATE LOBBY_USER SET BLOCKED = 1 WHERE PLAYER_ID in (select player_id from TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE)#
--
-- truncate TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE#
--
-- INSERT into TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE (player_id, correct_value)
-- SELECT slave.PLAYER_ID, master.blocked from strataproddw.LOBBY_USER slave inner join strataprod.LOBBY_USER master
--   on slave.PLAYER_ID = master.PLAYER_ID
--   where slave.blocked <> master.blocked and master.blocked = 0#
--
-- UPDATE LOBBY_USER SET BLOCKED = 0 WHERE PLAYER_ID in (select player_id from TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE)#
--
-- drop table TEMP_LOBBY_USERS_WRONG_BLOCKED_VALUE#
--
--