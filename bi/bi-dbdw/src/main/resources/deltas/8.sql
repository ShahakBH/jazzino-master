DROP PROCEDURE IF EXISTS create_index_if_not_exists#
CREATE PROCEDURE create_index_if_not_exists()
BEGIN

	if (select count(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_name = 'AUDIT_COMMAND' and index_name = 'IDX_AUDIT_COMMAND_AUDIT_TS') = 0 then
		alter table AUDIT_COMMAND add index IDX_AUDIT_COMMAND_AUDIT_TS(AUDIT_TS);
	end if; 

	if (select count(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_name = 'ACCOUNT_SESSION' and	index_name = 'IDX_ACCOUNT_SESSION_TSSTARTED') = 0 then
		alter table ACCOUNT_SESSION ADD index IDX_ACCOUNT_SESSION_TSSTARTED(TSSTARTED);
	end if; 
END#
call create_index_if_not_exists()#
DROP PROCEDURE IF EXISTS create_index_if_not_exists#