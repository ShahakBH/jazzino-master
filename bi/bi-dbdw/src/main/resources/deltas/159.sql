DROP TABLE IF EXISTS `strataproddw`.`SCHEDULED_TASK`#

CREATE TABLE `strataproddw`.`SCHEDULED_TASK` (
  `NAME` varchar(25),
  `INFO` varchar(255),
  `RUN_STATE` int(5),
  `LAST_RUN` datetime,
  `STATUS` int(1),
  PRIMARY KEY (`NAME`)
)#
