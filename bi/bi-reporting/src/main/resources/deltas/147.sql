--used as primary source for whether a cashier is a purchase or an offer
drop table if exists cashiers ;
create table cashiers(
     cashier_name varchar(255) NOT NULL PRIMARY KEY,
     is_purchase boolean not null
);

GRANT SELECT ON cashiers TO GROUP READ_ONLY;
GRANT ALL ON cashiers TO GROUP READ_WRITE;
GRANT ALL ON cashiers TO GROUP SCHEMA_MANAGER;

insert into cashiers values
('Zong',true),
('WorldPay',true),
('Wirecard',true),
('FACEBOOK',true),
('PayPal',true),
('Paypal',true),
('PayPal-WPP',true),
('PayPal DG',true),
('iTunesSandbox',true),
('iTunes',true),
('GoogleCheckout',true),
('Amazon',true);

insert into cashiers values
('FacebookEarnChips',false),
('FLURRY_MOBILE',false),
('radium',false),
('TAPJOY_ANDROID',false),
('TAPJOY_IOS',false),
('Trialpay',false);
