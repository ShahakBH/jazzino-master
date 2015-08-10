create view registrations as
select
	registration_date::date,
	registration_platform,
	count(1) num_registrations
from player
group by 1, 2;

GRANT SELECT ON registrations TO GROUP READ_ONLY;
GRANT ALL ON registrations TO GROUP READ_WRITE;
GRANT ALL ON registrations TO GROUP SCHEMA_MANAGER;
