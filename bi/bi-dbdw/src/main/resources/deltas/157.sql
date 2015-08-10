alter table EXTERNAL_TRANSACTION
modify column message_timestamp
timestamp not null default current_timestamp#
