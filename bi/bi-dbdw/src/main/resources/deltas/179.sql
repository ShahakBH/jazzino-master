drop procedure if exists deleteSentOrExpiredAppRequests#

-- delete expired or sent app requests that are at least n months old
create procedure deleteSentOrExpiredAppRequests(ageInMonths int)
begin
  declare done int default false;
  declare deleteDate datetime default date_sub(now(), interval ageInMonths month);
  declare minAppRequestTargetId, maxAppRequestTargetId int;
  declare appRequestId int;
  declare appRequestIdCursor cursor for select id from APP_REQUEST where expiry_dt < deleteDate or sent < deleteDate;
  declare continue handler for not found set done = 1;

  open appRequestIdCursor;
  readAppRequestIdLoop: loop
      fetch appRequestIdCursor into appRequestId;
       if done then
          leave readAppRequestIdLoop;
       end if;
       
       select min(id), max(id) into minAppRequestTargetId, maxAppRequestTargetId from APP_REQUEST_TARGET where app_request_id = appRequestId;
	   while minAppRequestTargetId <= maxAppRequestTargetId do
	      start transaction;
		  delete from APP_REQUEST_TARGET where app_request_id = appRequestId and id <= minAppRequestTargetId + 1000;
		  commit;
		  set minAppRequestTargetId = minAppRequestTargetId + 1000;
	   end while;       
	   delete from APP_REQUEST where id = appRequestId;  
  end loop;
  close appRequestIdCursor;
end
#

drop event if exists evt_deleteAppRequests#

create event evt_deleteAppRequests on schedule every 10 day_hour
comment 'Delete expired/sent application requests'
do call deleteSentOrExpiredAppRequests(2)#