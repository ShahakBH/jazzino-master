-- WEB-4513 - fix data types

-- Some of the data exceeds the 65k limit on observable_status
alter table audit_closed_game alter observable_status type text;
