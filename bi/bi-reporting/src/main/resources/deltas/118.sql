CREATE TABLE adnet_mappings(
  referrer character varying(255),
  registration_adnet character varying(32),
  PRIMARY KEY(registration_adnet));

GRANT SELECT ON adnet_mappings TO GROUP READ_ONLY;
GRANT ALL ON adnet_mappings TO GROUP READ_WRITE;
GRANT ALL ON adnet_mappings TO GROUP SCHEMA_MANAGER;

insert into adnet_mappings values
('Blog_WD-QA', 'Organic'),
('and_wd_appgratis_uk', 'Android - Appgratis'),
('and_wd_appsfire_us', 'Android - Appsfire'),
('and_wd_manage_nordic', 'Android - Manage');

--doesn't need maintaining as it's completely rebuilt every time.
