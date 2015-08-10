-- needs to a proc since index already added to prod
DROP PROCEDURE IF EXISTS create_index_on_app_request_target_if_not_exists#
CREATE PROCEDURE create_index_on_app_request_target_if_not_exists()
BEGIN

	if (select count(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_name = 'APP_REQUEST_TARGET' and index_name = 'IDX_EXTERNAL') = 0 then
		ALTER TABLE APP_REQUEST_TARGET ADD INDEX IDX_EXTERNAL (EXTERNAL_ID);
	end if;
END
#
call create_index_on_app_request_target_if_not_exists()
#
DROP PROCEDURE IF EXISTS create_index_on_app_request_target_if_not_exists
#