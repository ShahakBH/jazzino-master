--
-- Backoffice requires the date when players last played. This info is extracted to table 'LAST_PLAYED'. This
--  is now necessary since 'old' audit data will be dropped.
--  Procedure extractLastPlayed should be executed at least every hour.
--
DROP TABLE IF EXISTS LAST_PLAYED#

CREATE TABLE LAST_PLAYED (
  ACCOUNT_ID INT NOT NULL,
  GAME_TYPE VARCHAR(255) NOT NULL,
  LAST_PLAYED DATETIME NOT NULL,
  PRIMARY KEY (ACCOUNT_ID, GAME_TYPE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#

-- we don't have audit data prior to march 2011 (tables were partitioned towards the end of march)
replace into rpt_report_status values ('extractLastPlayed', '2011-03-01 00:00:00')#

DROP PROCEDURE IF EXISTS extractLastPlayed#
--
-- Inserts (or replaces) the last played time per game per account
--
CREATE PROCEDURE extractLastPlayed()
begin
	declare runtime timestamp;

	set @runtime = now();

	select action_ts into @lastRunTime from rpt_report_status where report_action = 'extractLastPlayed';

	replace into LAST_PLAYED (account_id, game_type, last_played)
	  select account_id, game_type, max(audit_ts)
      from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
      where ac.audit_ts >= @lastRunTime
      group by account_id, game_type;

	update rpt_report_status set action_ts = @runtime where report_action = 'extractLastPlayed';
end;
#

--
-- Similarly, for player dashboard statistics, we need to keep a running total for chips staked/returned/adjusted etc
-- This table will only contain info for txs on or after 2011-03-25 (when tables where partitioned)
--
DROP TABLE IF EXISTS CHIP_SUMMARY#
create table CHIP_SUMMARY (
  ACCOUNT_ID int not null,
  TOTAL_STAKED decimal(64,4),
  TOTAL_RETURNED decimal(64,4),
  TOTAL_ADJUSTMENTS decimal(64,4),
  LAST_TRANSACTION_LOG_ID int not null,
  primary key (ACCOUNT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#

alter table rpt_report_status add column val varchar(255)#
alter table rpt_report_status add column val_desc varchar(2000)#

-- we don't have audit data prior to march 2011 (tables were partitioned towards the end of march)
replace into rpt_report_status values ('extractChipSummary', '2011-03-01 00:00:00', -1, 'TRANSACTION_LOG_ID last processed')#

DROP PROCEDURE IF EXISTS extractChipSummary#
--
-- updates chips staked/returned/adjusted per account
--
CREATE PROCEDURE extractChipSummary()
begin
	declare runtime timestamp;
	declare maxTxLogId, lastTxLogIdProcessed int;

    -- not needed by update but for consistency of rows in rpt_report_status
	set @runtime = now();

    select max(transaction_log_id) into @maxTxLogId from TRANSACTION_LOG;
	select val into @lastTxLogIdProcessed from rpt_report_status where report_action = 'extractChipSummary';

	insert into CHIP_SUMMARY (account_id, total_staked, total_returned, total_adjustments, last_transaction_log_id)
        select account_id,
            -sum(if(transaction_type = 'Stake', amount, 0)),
            sum(if(transaction_type = 'Return', amount, 0)),
            sum(if(transaction_type = 'Adjustment', amount, 0)),
            max(transaction_log_id)
        from (
            SELECT account_id, AMOUNT,TRANSACTION_TYPE, transaction_log_id
            FROM TRANSACTION_LOG
            where transaction_log_id > @lastTxLogIdProcessed and transaction_log_id <= @maxTxLogId
              and TRANSACTION_TYPE in ('Stake', 'Return', 'Adjustment')
        ) TXS
        group by account_id
    on duplicate key
        update total_staked = total_staked + values(total_staked),
            total_returned = total_returned + values(total_returned),
            total_adjustments = total_adjustments + values(total_adjustments),
            last_transaction_log_id = values(last_transaction_log_id);

	update rpt_report_status set action_ts = @runtime, val = @maxTxLogId where report_action = 'extractChipSummary';
end;
#

