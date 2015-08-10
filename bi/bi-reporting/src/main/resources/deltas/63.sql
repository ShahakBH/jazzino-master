create table cashier_platform(
	cashier_name varchar(32) not null primary key,
	cashier_platform varchar(32) not null
);

insert into cashier_platform values
('Wirecard', 'WEB'),
('PayPal', 'WEB'),
('PayPal-WPP', 'WEB'),
('PayPal DG', 'WEB'),
('Zong', 'WEB'),
('radium', 'WEB'),
('Trialpay', 'WEB'),
('TAPJOY_IOS', 'IOS'),
('iTunes', 'IOS'),
('FLURRY_MOBILE', 'IOS'),
('FACEBOOK', 'FACEBOOK_CANVAS'),
('FacebookEarnChips', 'FACEBOOK_CANVAS'),
('GoogleCheckout', 'ANDROID'),
('TAPJOY_ANDROID', 'ANDROID');

GRANT SELECT ON cashier_platform TO GROUP READ_ONLY;
GRANT ALL ON cashier_platform TO GROUP READ_WRITE;
GRANT ALL ON cashier_platform TO GROUP SCHEMA_MANAGER;