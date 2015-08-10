INSERT INTO `GAME_CONFIGURATION_PROPERTY` (GAME_ID, PROPERTY_NAME, PROPERTY_VALUE)
select 'SLOTS', 'flashWMode', 'direct'
from GAME_CONFIGURATION_PROPERTY
where not exists (select * from GAME_CONFIGURATION_PROPERTY where game_id = 'SLOTS' and property_name = 'flashWMode')
limit 1#