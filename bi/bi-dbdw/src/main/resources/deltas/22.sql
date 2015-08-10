alter table LAST_PLAYED change ACCOUNT_ID PLAYER_ID bigint(20)#

--
-- Modify last played to reflect column name change
--
DROP PROCEDURE IF EXISTS extractLastPlayed#
CREATE PROCEDURE extractLastPlayed()
begin
	declare runtime timestamp;
	declare lastAutoId int;
	declare maxAutoId int;

	set @runtime = now();

	select action_ts, val into @lastRunTime, @lastAutoId from rpt_report_status where report_action = 'extractLastPlayed';
    select max(auto_id) into @maxAutoId from AUDIT_COMMAND;

	replace into LAST_PLAYED (player_id, game_type, last_played)
	  select account_id, game_type, max(audit_ts)
      from AUDIT_COMMAND ac join strataprod.TABLE_INFO ti on ac.table_id = ti.table_id
      where ac.auto_id > @lastAutoId and ac.auto_id <= @maxAutoId
        and command_type not in ('Leave', 'GetStatus')
      group by account_id, game_type;

	update rpt_report_status set action_ts = @runtime, val = @maxAutoId where report_action = 'extractLastPlayed';
end;
#
