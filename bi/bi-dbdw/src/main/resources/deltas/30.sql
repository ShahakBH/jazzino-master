-- schedule management of ACCOUNT_SESSION partitions
drop event if exists manageAccountSessionPartitions#
-- CREATE EVENT manageAccountSessionPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:00:00'
--     DO call manageDateOrTimestampPartitions('ACCOUNT_SESSION', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 30)#

-- schedule management of AUDIT_COMMAND partitions
drop event if exists manageAuditCommandPartitions#
-- CREATE EVENT manageAuditCommandPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:01:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_COMMAND', 'TO_DAYS', '%Y-%m-%d', 30)#

-- schedule management of AUDIT_CLOSED_GAME partitions
drop event if exists manageAuditClosedGamePartitions#
-- CREATE EVENT manageAuditClosedGamePartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:02:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME', 'TO_DAYS', '%Y-%m-%d', 30)#

-- schedule management of AUDIT_CLOSED_GAME_PLAYER partitions
drop event if exists manageAuditClosedGamePlayerPartitions#
-- CREATE EVENT manageAuditClosedGamePlayerPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:03:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME_PLAYER', 'TO_DAYS', '%Y-%m-%d', 30)#

-- schedule management of TRANSACTION_LOG partitions
drop event if exists manageTransactionLogPartitions#
-- CREATE EVENT manageTransactionLogPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:04:00'
--     DO call manageDateOrTimestampPartitions('TRANSACTION_LOG', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 30)#

-- invoke procs to ensure initial drop of partitions is done as part of the release (takes a few seconds to drop a partition)
-- call manageDateOrTimestampPartitions('ACCOUNT_SESSION', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 30)#
-- call manageDateOrTimestampPartitions('AUDIT_COMMAND', 'TO_DAYS', '%Y-%m-%d', 30)#
-- call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME', 'TO_DAYS', '%Y-%m-%d', 30)#
-- call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME_PLAYER', 'TO_DAYS', '%Y-%m-%d', 30)#
-- call manageDateOrTimestampPartitions('TRANSACTION_LOG', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 30)#
