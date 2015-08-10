DROP TABLE IF EXISTS PROMOTION_DEFINITION#
CREATE TABLE PROMOTION_DEFINITION (
  ID int(11) NOT NULL auto_increment,
  CAMPAIGN_ID int(11),
  name varchar(255),
  VALID_FOR_HOURS smallint,
  PRIORITY tinyint,
  PLATFORMS varchar(255),
  primary key(ID)
)#