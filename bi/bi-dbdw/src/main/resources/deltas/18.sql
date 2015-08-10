DROP TABLE IF EXISTS rpt_account_activity#

create table rpt_account_activity (
  ACCOUNT_ID int not null,
  AUDIT_DATE date not null,
  GAME_TYPE varchar(255),
  primary key (ACCOUNT_ID, AUDIT_DATE, GAME_TYPE),
  key IDX_AUDIT_DATE (AUDIT_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8#