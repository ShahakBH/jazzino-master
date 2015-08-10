-- update peoples permissions to use new platform enum
delete from OPERATIONS_PLATFORM where platform = 'ROBOT'#
delete from OPERATIONS_PLATFORM where platform = 'MOBILE'#
update ignore OPERATIONS_PLATFORM set platform = 'WEB' where platform in ('FACEBOOK', 'YAZINO_WEB')#
delete from OPERATIONS_PLATFORM where platform in ('FACEBOOK', 'YAZINO_WEB')#
