DROP VIEW IF EXISTS rpt_player_sources#
-- CREATE VIEW rpt_player_sources AS
-- SELECT p.USER_ID AS USER_ID, p.PLAYER_ID AS PLAYER_ID,
-- CASE WHEN at.AD_CODE IS NULL THEN (CASE WHEN REFERRAL_ID IS NULL THEN 'Natural' ELSE 'Invited' END) ELSE at.AD_CODE END AS SOURCE,
-- p.ACCOUNT_ID AS ACCOUNT_ID, p.tscreated AS TSCREATED
-- FROM strataprod.PLAYER p
-- JOIN strataprod.LOBBY_USER lu ON p.USER_ID=lu.USER_ID
-- LEFT JOIN AD_TRACKING at ON p.USER_ID=at.USER_ID
-- ORDER BY p.USER_ID DESC#
