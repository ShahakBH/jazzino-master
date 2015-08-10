-- WEB-4174 - remove legacy Maximiles objects

DROP PROCEDURE IF EXISTS mm_export_initial_games#
DROP PROCEDURE IF EXISTS mm_export_latest_invites#
DROP PROCEDURE IF EXISTS mm_export_latest_leaderboard#
DROP PROCEDURE IF EXISTS mm_export_latest_levels#
DROP PROCEDURE IF EXISTS mm_export_latest_logins#
DROP PROCEDURE IF EXISTS mm_export_latest_purchases#

DROP VIEW IF EXISTS mm_external_transactions#
DROP VIEW IF EXISTS mm_chips#

DROP TABLE IF EXISTS CRM_PLAYER_STATUS#
DROP TABLE IF EXISTS PROVIDER_REJECTED_EMAIL_ADDRESSES#
