alter table CAMPAIGN_DEFINITION add column delay_notifications boolean not null default false#
alter table CAMPAIGN_RUN add column last_rerun_ts timestamp null#