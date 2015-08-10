drop event if exists evt_recreate_promotion_reports#

create event evt_recreate_promotion_reports
on schedule every 1 day starts '2012-09-08 10:00:00'
do call recreate_promotion_reports()#
