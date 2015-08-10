-- For reinitialization, the rpt_report_status starting point for the extractChipSummary should be set to 2141316356

DROP PROCEDURE IF EXISTS extractChipSummary#
--
-- updates chips staked/returned/adjusted per account
--
CREATE PROCEDURE extractChipSummary()
begin
	declare runtime timestamp;
	declare maxTxLogId, lastTxLogIdProcessed bigint(20);

    -- not needed by update but for consistency of rows in rpt_report_status
	set @runtime = now();
	
	if get_lock('strataproddw.fill_extract_chip_summary', 0) = 1 then
	    select max(transaction_log_id) into @maxTxLogId from TRANSACTION_LOG;
		select val into @lastTxLogIdProcessed from rpt_report_status where report_action = 'extractChipSummary';
	
		insert into CHIP_SUMMARY (account_id, total_staked, total_returned, total_adjustments)
	        SELECT account_id, -sum(if(transaction_type = 'Stake', amount, 0)),
	            sum(if(transaction_type = 'Return', amount, 0)),
	            sum(if(transaction_type = 'Adjustment', amount, 0))
	            FROM TRANSACTION_LOG
	            where transaction_log_id > @lastTxLogIdProcessed and transaction_log_id <= @maxTxLogId
	              and TRANSACTION_TYPE in ('Stake', 'Return', 'Adjustment')
				GROUP BY account_id	              
	    on duplicate key
	        update total_staked = total_staked + values(total_staked),
	            total_returned = total_returned + values(total_returned),
	            total_adjustments = total_adjustments + values(total_adjustments);
	
		update rpt_report_status set action_ts = @runtime, val = @maxTxLogId where report_action = 'extractChipSummary';
		
		do release_lock('strataproddw.fill_extract_chip_summary');
	end if;
end;
#

DROP EVENT IF EXISTS extractChipSummary#
DROP EVENT IF EXISTS evtExtractChipSummary#
CREATE EVENT evtExtractChipSummary ON SCHEDULE EVERY 10 MINUTE DO call extractChipSummary()#


