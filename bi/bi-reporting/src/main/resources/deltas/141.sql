alter table campaign_run_audit drop constraint campaign_run_audit_pkey cascade;

alter table campaign_run_audit alter column status set not null;

alter table campaign_run_audit add primary key(CAMPAIGN_ID,RUN_ID,status);
