-- WEB-4784
alter table lobby_user alter column partner_id set default 'YAZINO';
update lobby_user set partner_id='YAZINO' where partner_id='PLAY_FOR_FUN';
