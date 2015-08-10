DROP VIEW IF EXISTS mm_last_levels#
DROP VIEW IF EXISTS mm_tournaments_log#
-- This is needed by com.yazino.maximiles.export.DailyChipReportsDao
-- DROP VIEW IF EXISTS mm_chips#
-- This is needed by com.yazino.maximiles.export.PlayerPurchasesDao
-- DROP VIEW IF EXISTS mm_external_transactions#
DROP VIEW IF EXISTS mm_initial_games#
DROP VIEW IF EXISTS mm_last_leaderboard#
DROP VIEW IF EXISTS mm_last_played#
DROP VIEW IF EXISTS mm_leaderboard_state#
DROP VIEW IF EXISTS mm_logins#
DROP VIEW IF EXISTS mm_total_invites#
DROP VIEW IF EXISTS mm_total_invites#
DROP VIEW IF EXISTS mm_tournaments_participation#

DROP PROCEDURE IF EXISTS mm_initial_games#
DROP PROCEDURE IF EXISTS mm_export_latest_chips#
DROP PROCEDURE IF EXISTS mm_latest_invites#
DROP PROCEDURE IF EXISTS mm_latest_leaderboard#
DROP PROCEDURE IF EXISTS mm_latest_levels#
DROP PROCEDURE IF EXISTS mm_latest_logins#
DROP PROCEDURE IF EXISTS mm_latest_purchases#
DROP PROCEDURE IF EXISTS mm_export_latest_tournaments#