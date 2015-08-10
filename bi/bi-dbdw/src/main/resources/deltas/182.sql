drop procedure if exists deleteSentOrExpiredAppRequests#

-- delete expired or sent app requests that are at least n months old
create procedure deleteSentOrExpiredAppRequests(ageInMonths int)
begin
  declare done int default false;
  declare minAppRequestTargetId, maxAppRequestTargetId int;
  declare appRequestId int;
  -- delete targets from requests that have been sent and expired at least 1 day ago  (to allow for delete requests to be sent)
  declare appRequestIdCursor cursor for select id from APP_REQUEST where sent is not null and expiry_dt < date_sub(now(), interval 1 day);
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
	   -- marketing required history of app requests to be kept for 'ageInMonths' months - targets are not required
	   delete from APP_REQUEST where id = appRequestId and created < date_sub(now(), interval ageInMonths month);
  end loop;
  close appRequestIdCursor;
end
#

drop event if exists deleteAppRequests#

create event deleteAppRequests ON SCHEDULE EVERY 1 DAY STARTS '2012-11-12 10:00:00'
comment 'Delete expired/sent application requests'
do call deleteSentOrExpiredAppRequests(2)#