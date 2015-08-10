-- WEB-4578 - remove audit tables that now live in reporting schema

DROP VIEW IF EXISTS $agg_status#
DROP VIEW IF EXISTS $max_transaction_id#
DROP VIEW IF EXISTS GAMES_YESTERDAY#
DROP VIEW IF EXISTS GAME_STATS_YESTERDAY#
DROP VIEW IF EXISTS PLAYER_PURCHASES#
DROP VIEW IF EXISTS UNIQUE_BUYERS_SOURCE_YESTERDAY#
DROP VIEW IF EXISTS UNIQUE_BUYERS_YESTERDAY#

DROP EVENT IF EXISTS evtFillDailyMailStats#
DROP EVENT IF EXISTS evt_calc_agg_transaction_log_by_date_gvt_acc_nb#
DROP EVENT IF EXISTS evtExtractAccountActivity#
DROP EVENT IF EXISTS evtExtractChipSummary#
DROP EVENT IF EXISTS rptExtractExternalTransactions#
DROP EVENT IF EXISTS rptExtractExternalTransactionsPerCurrency#


DROP TABLE IF EXISTS rpt_external_transaction#
DROP TABLE IF EXISTS rpt_external_transaction_per_currency#

DROP EVENT IF EXISTS manageAccountSessionPartitions#
DROP EVENT IF EXISTS manageAuditClosedGamePartitions#
DROP EVENT IF EXISTS manageAuditClosedGamePlayerPartitions#
DROP EVENT IF EXISTS manageAuditCommandPartitions#
DROP EVENT IF EXISTS manageTransactionLogPartitions#
DROP PROCEDURE IF EXISTS manageDateOrTimestampPartitions#

DROP PROCEDURE IF EXISTS extractChipSummary#
DROP PROCEDURE IF EXISTS calc_agg_transaction_log_by_date_gvt_acc#
DROP PROCEDURE IF EXISTS calc_agg_transaction_log_by_date_gvt_acc_nb#
DROP TABLE IF EXISTS TRANSACTION_LOG#

DROP PROCEDURE IF EXISTS extractAccountActivity#
DROP TABLE IF EXISTS AUDIT_COMMAND#

DROP PROCEDURE IF EXISTS rptExtractExternalTransactions#
DROP PROCEDURE IF EXISTS rptExtractExternalTransactionsPerCurrency#
DROP PROCEDURE IF EXISTS external_transactions_inserts#
DROP TABLE IF EXISTS EXTERNAL_TRANSACTION#

DROP TABLE IF EXISTS AUDIT_CLOSED_GAME#
DROP TABLE IF EXISTS AUDIT_CLOSED_GAME_PLAYER#

DROP PROCEDURE IF EXISTS account_inserts#
DROP PROCEDURE IF EXISTS scanAccountSessions#
DROP TABLE IF EXISTS ACCOUNT_SESSION#

DROP EVENT IF EXISTS evt_registration_by_date_platform_game_type_source#
DROP PROCEDURE IF EXISTS registration_by_date_platform_game_type_source#

