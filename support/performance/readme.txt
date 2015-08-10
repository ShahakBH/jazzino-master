

* Force user passwords to 'letmein'

UPDATE YAZINO_LOGIN SET PASSWORD_HASH='4gn+bHlQsXNR118FJyH3grYgfYg=', SALT=UNHEX('D4080D7905338AAF'), PASSWORD_TYPE='PBKDF2' where email_address not like '%@yazino.com';

* Making "YAZINO_ROBOT" users grinder-friendly:

INSERT INTO YAZINO_LOGIN(EMAIL_ADDRESS, PASSWORD_HASH,PASSWORD_TYPE,SALT,USER_ID) SELECT CONCAT(DISPLAY_NAME, '@example.com'), '4gn+bHlQsXNR118FJyH3grYgfYg=','PBKDF2', UNHEX('D4080D7905338AAF'), USER_ID from LOBBY_USER WHERE PROVIDER_NAME='YAZINO_ROBOT';
UPDATE LOBBY_USER SET PROVIDER_NAME="YAZINO" WHERE PROVIDER_NAME='YAZINO_ROBOT';
UPDATE ACCOUNT SET BALANCE="10000000000" WHERE ACCOUNT_ID IN (SELECT ACCOUNT_ID FROM PLAYER WHERE USER_ID IN (SELECT USER_ID FROM YAZINO_LOGIN WHERE YAZINO_LOGIN.PASSWORD_TYPE = 'PBKDF2' AND PASSWORD_HASH= '4gn+bHlQsXNR118FJyH3grYgfYg='));

* avg cmd/sec last 20 minutes
select audit_ts as time, count(auto_id) / 60 from AUDIT_COMMAND where audit_ts >= now() - interval 20 minute group by floor(unix_timestamp(audit_ts) / 60);

* cmd/sec last 10 sec
select audit_ts as time, count(auto_id) from AUDIT_COMMAND where audit_ts >= now() - interval 10 second group by audit_ts;

* cmd frequencies for last minute
select COMMAND_TYPE, count(*) from AUDIT_COMMAND where audit_ts >= now() - interval 1 minute group by COMMAND_TYPE;


* # players sending commands
select count(distinct ACCOUNT_ID) from AUDIT_COMMAND where audit_ts >= now() - interval 10 second;


insert into YAZINO_LOGIN (email_address, password_hash, user_id)
select email_address, 'DRB9CfW75Ayt495ccenptw==', user_id
from LOBBY_USER
where user_id > 950000
and user_id not in (select user_id from YAZINO_LOGIN);


* distribution of players based on bets on last 30 seconds
(e.g. if we expect one bet every 5 seconds, the ideal number of bets in 30 seconds is 6)

select num_commands, count(1)
from (
select account_id, count(1) num_commands
from AUDIT_COMMAND
where audit_ts >= now() - interval 30 second
and command_type = 'Bet'
group by account_id) xxx
group by num_commands
order by 2 desc;
