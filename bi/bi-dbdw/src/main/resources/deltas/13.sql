drop procedure if exists manageDateOrTimestampPartitions#

-- ----------------------------------------------------------------------------------------
-- Ensures that partitions for the next 7 days exist for the given table.
-- Drops partitions older than given number of days.
-- It's required that the table is already partitioned and has at least one partition
-- other than the default.
--
-- @tableName
--      table to create and drop partitions on
-- @rangeFunction
--      the range function to use e.g. UNIX_TIMESTAMP or TO_DAYS. Usually the
--      range function should be the same as the one used to create the partitioned table.
-- @rangeDateFormatStr
--      the format string used by the rangeFunction e.g. '%Y-%m-%d 00:00:00'
--      (suitable for timestamps) or '%Y-%m-%d' (for datetimes etc).
-- @partitionDaysToKeep
--      indicates the number days of partitions to keep. Partitions older than this
--      number of days will be dropped.
--      If <= 0 then no partitions are dropped.
-- ----------------------------------------------------------------------------------------
-- CREATE PROCEDURE manageDateOrTimestampPartitions(
--     in tableName varchar(255),
--     in rangeFunction varchar(100),
--     in rangeDateFormatStr varchar(100),
--     in partitionDaysToKeep int)
-- begin
--     declare latestPartitionDate, newPartitionDate, lastPartitionDateToCreate, oldPartitionDate date;
--     declare oldestPartitionAsDays int;
--     declare alterTableSql, dropPartitionsSql longtext;
--     declare debug tinyint;
--
--     set @debug = false;
--
--     -- get the dates of the oldest and newest partitions
--     select min(partitionDate), max(partitionDate) into @oldestPartitionDate, @latestPartitionDate
--     from (
--         select STR_TO_DATE(partition_name,'p%Y%m%d') partitionDate
--         from information_schema.PARTITIONS
--         where table_schema = 'strataproddw' and table_name = tableName
--         and partition_description != 'MAXVALUE'
--     ) partitionDateTable;
--
--     -- ensure that partitions for next 7 days exist, if they do not create them
--     set @lastPartitionDateToCreate = date(date_add(now(), interval 7 day));
--     if (@lastPartitionDateToCreate > @latestPartitionDate) then
--         set @alterTableSql = concat('ALTER TABLE ', tableName, ' REORGANIZE PARTITION pDefault INTO (');
--         set @newPartitionDate = @latestPartitionDate;
--         repeat
--             set @newPartitionDate = date(date_add(@newPartitionDate, interval 1 day));
--             set @alterTableSql = concat(@alterTableSql,
--                                         'PARTITION ',
--                                         date_format(@newPartitionDate, 'p%Y%m%d'),
--                                         ' values less than (',
--                                         rangeFunction,
--                                         '(\'',
--                                         date_format(date_add(@newPartitionDate, interval 1 day), rangeDateFormatStr),
--                                         '\')),');
--         until @newPartitionDate >= @lastPartitionDateToCreate
--         end repeat;
--
--         set @alterTableSql = concat(@alterTableSql, 'PARTITION pDefault values less than (MAXVALUE))');
--
--         prepare stmt from @alterTableSql;
--         execute stmt;
--         deallocate prepare stmt;
--     end if;
--
--     -- drop partions older than dropPartitionsOlderThanDays
--     if (partitionDaysToKeep > 0) then
--         set @oldestPartitionAsDays = to_days(@oldestPartitionDate);
--         while (@oldestPartitionAsDays < (to_days(now()) - partitionDaysToKeep)) do
--             set @dropPartitionsSql = concat('ALTER TABLE ', tableName, ' DROP PARTITION ', date_format(from_days(@oldestPartitionAsDays), 'p%Y%m%d'));
--             prepare stmt from @dropPartitionsSql;
--             execute stmt;
--             deallocate prepare stmt;
--             set @oldestPartitionAsDays = @oldestPartitionAsDays + 1;
--         end while;
--     end if;
--
--     if (@debug = true) then
--         select tableName, @alterTableSql, @latestPartitionDate, @newPartitionDate, @lastPartitionDateToCreate, @oldestPartitionAsDays, to_days(now());
--     end if;
-- end
-- #

-- schedule management of ACCOUNT_SESSION partitions
drop event if exists manageAccountSessionPartitions#
-- CREATE EVENT manageAccountSessionPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:00:00'
--     DO call manageDateOrTimestampPartitions('ACCOUNT_SESSION', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 0)#

-- schedule management of AUDIT_COMMAND partitions
drop event if exists manageAuditCommandPartitions#
-- CREATE EVENT manageAuditCommandPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:01:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_COMMAND', 'TO_DAYS', '%Y-%m-%d', 0)#

-- schedule management of AUDIT_CLOSED_GAME partitions
drop event if exists manageAuditClosedGamePartitions#
-- CREATE EVENT manageAuditClosedGamePartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:02:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME', 'TO_DAYS', '%Y-%m-%d', 0)#

-- schedule management of AUDIT_CLOSED_GAME_PLAYER partitions
drop event if exists manageAuditClosedGamePlayerPartitions#
-- CREATE EVENT manageAuditClosedGamePlayerPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:03:00'
--     DO call manageDateOrTimestampPartitions('AUDIT_CLOSED_GAME_PLAYER', 'TO_DAYS', '%Y-%m-%d', 0)#

-- schedule management of TRANSACTION_LOG partitions
drop event if exists manageTransactionLogPartitions#
-- CREATE EVENT manageTransactionLogPartitions ON SCHEDULE EVERY 1 DAY STARTS '2011-04-01 10:04:00'
--     DO call manageDateOrTimestampPartitions('TRANSACTION_LOG', 'UNIX_TIMESTAMP', '%Y-%m-%d 00:00:00', 0)#
