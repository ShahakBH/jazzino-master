--ManagementReportAggregator stores the materialised data here for the management report
drop table if exists MANAGEMENT_REPORT;

create table MANAGEMENT_REPORT(
  activity_date date not null primary key,
  registrations integer,
  players integer,
  revenue numeric(64,4),
  purchases integer
);

GRANT SELECT ON MANAGEMENT_REPORT TO GROUP READ_ONLY;
GRANT ALL ON MANAGEMENT_REPORT TO GROUP READ_WRITE;
GRANT ALL ON MANAGEMENT_REPORT TO GROUP SCHEMA_MANAGER;
