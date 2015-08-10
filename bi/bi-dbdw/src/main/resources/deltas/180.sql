drop event if exists evt_deleteAppRequests#

create event evt_deleteAppRequests ON SCHEDULE EVERY 1 DAY STARTS '2012-11-12 10:00:00'
comment 'Delete expired/sent application requests'
do call deleteSentOrExpiredAppRequests(2)#

