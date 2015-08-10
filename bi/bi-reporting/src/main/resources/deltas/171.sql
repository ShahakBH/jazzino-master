drop table if exists archive_transaction_log;
drop table if exists archive_audit_command;
drop table if exists archive_client_log ;
drop table if exists archive_campaign_notification_audit ;

create table archive_transaction_log as select * From transaction_log limit 0;
create table archive_audit_command as select * From audit_command limit 0;
create table archive_client_log as select * From client_log limit 0;
create table archive_campaign_notification_audit as select * From campaign_notification_audit limit 0;
--no indices or constraints

grant select on archive_transaction_log to group read_only;
grant all on archive_transaction_log to group read_write;
grant all on archive_transaction_log to group schema_manager;

grant select on archive_audit_command to group read_only;
grant all on archive_audit_command to group read_write;
grant all on archive_audit_command to group schema_manager;

grant select on archive_client_log to group read_only;
grant all on archive_client_log to group read_write;
grant all on archive_client_log to group schema_manager;

grant select on archive_campaign_notification_audit to group read_only;
grant all on archive_campaign_notification_audit to group read_write;
grant all on archive_campaign_notification_audit to group schema_manager;
