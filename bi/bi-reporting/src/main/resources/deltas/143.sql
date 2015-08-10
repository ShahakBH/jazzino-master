alter table stg_lobby_user alter column provider_name drop not null;
alter table stg_lobby_user alter column    rpx_provider  drop not null;
alter table stg_lobby_user alter column    blocked drop not null;
alter table stg_lobby_user alter column    status drop not null;
alter table stg_lobby_user alter column    guest_status drop not null;

alter table lobby_user alter column provider_name drop not null;
alter table lobby_user alter column    rpx_provider  drop not null;
alter table lobby_user alter column    blocked drop not null;
alter table lobby_user alter column    status drop not null;
alter table lobby_user alter column    guest_status drop not null;

alter table lobby_user add column account_id numeric(16,2) ;
alter table lobby_user add column registration_referrer   varchar(255);
alter table lobby_user add column registration_platform   varchar(32);
alter table lobby_user add column registration_game_type  varchar(32);

alter table stg_lobby_user add column account_id numeric(16,2) ;
alter table stg_lobby_user add column registration_referrer   varchar(255);
alter table stg_lobby_user add column registration_platform   varchar(32);
alter table stg_lobby_user add column registration_game_type  varchar(32);

drop view ad_tracking_view;
drop view registrations;
drop view purchase_with_offers_view;
drop view purchase_view;
drop view player;
drop view external_transaction_view;

DROP  MATERIALIZED VIEW IF EXISTS external_transaction_mv;
CREATE MATERIALIZED VIEW external_transaction_mv
AS
 SELECT
    et.player_id,
    lu.account_id,
    lu.reg_ts::date AS registration_date,
    lu.registration_platform,
    lu.registration_referrer,
    min(date_trunc('day', et.message_ts)) OVER (PARTITION BY et.player_id) AS first_purchase_date,
    et.message_ts AS purchase_ts,
    et.amount,
    et.currency_code,
    et.amount / cr.rate AS amount_gbp,
    et.cashier_name,
    et.promo_id,
    et.amount_chips,
    et.transaction_type,
    et.game_type,
    COALESCE(et.platform, cp.cashier_platform) AS purchase_platform,
    rank() OVER (PARTITION BY et.player_id ORDER BY et.message_ts) AS purchase_num,
    et.external_transaction_status
 FROM external_transaction et
    LEFT JOIN currency_rates cr ON et.currency_code = cr.currency_code
    LEFT JOIN lobby_user lu ON et.player_id = lu.player_id
    LEFT JOIN cashier_platform cp ON et.cashier_name = cp.cashier_name

 WHERE (external_transaction_status = 'SUCCESS' or external_transaction_status = 'AUTHORISED')
;

CREATE INDEX external_transaction_mv_account_id_idx ON external_transaction_mv USING btree (account_id);
CREATE INDEX external_transaction_mv_player_id_idx ON external_transaction_mv USING btree (player_id);
CREATE INDEX external_transaction_mv_message_ts_idx ON external_transaction_mv USING btree (date_trunc('day', purchase_ts));

alter table external_transaction_mv owner to SCHEMA_MANAGER;

GRANT SELECT ON external_transaction_mv TO GROUP READ_ONLY;
GRANT ALL ON external_transaction_mv TO GROUP READ_WRITE;
GRANT ALL ON external_transaction_mv TO GROUP ADMIN;
GRANT ALL ON external_transaction_mv TO GROUP reporting;
GRANT ALL ON external_transaction_mv TO GROUP SCHEMA_MANAGER;

--
CREATE VIEW player
(
  player_id,
  account_id,
  balance,
  registration_date,
  registration_platform,
  registration_game_type,
  picture_location,
  country
)
AS
 SELECT u.player_id,
    u.account_id,
    a.balance,
    u.reg_ts AS registration_date,
    u.registration_platform,
    u.registration_game_type,
    u.picture_location,
    u.country
   FROM lobby_user u
   LEFT JOIN account a ON a.account_id = u.account_id;

GRANT SELECT ON player TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON player TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON player TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON player TO reporting;

--
CREATE VIEW ad_tracking_view as select * from external_transaction_mv; --this is wrong. it doesn't contain authorised

GRANT SELECT ON ad_tracking_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON ad_tracking_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON ad_tracking_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON ad_tracking_view TO reporting;

--
CREATE VIEW registrations
(
  registration_date,
  registration_platform,
  num_registrations
)
AS
 SELECT player.registration_date::date AS registration_date,
    player.registration_platform,
    count(1) AS num_registrations
   FROM player
  GROUP BY player.registration_date::date, player.registration_platform;

GRANT SELECT ON registrations TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON registrations TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON registrations TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON registrations TO reporting;

--
CREATE or replace VIEW purchase_with_offers_view as select * from external_transaction_mv;

GRANT SELECT ON purchase_with_offers_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON purchase_with_offers_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON purchase_with_offers_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON purchase_with_offers_view TO reporting;

--
CREATE or replace VIEW purchase_view as
 SELECT * from external_transaction_mv et
  WHERE (et.cashier_name::text = ANY (ARRAY['FACEBOOK'::character varying, 'GoogleCheckout'::character varying, 'PayPal'::character varying, 'PayPal-WPP'::character varying, 'Paypal'::character varying, 'iTunes'::character varying, 'PayPal DG'::character varying, 'Wirecard'::character varying, 'Zong'::character varying, 'WorldPay'::character varying, 'Amazon'::character varying]::text[]));

GRANT SELECT ON purchase_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON purchase_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON purchase_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON purchase_view TO reporting;

--

create or replace view player_purchase_daily as
select
	player_id,
	registration_date,
	purchase_platform,
	purchase_ts::date purchase_date,
	sum(amount_gbp) total_amount_gbp,
	count(1) num_purchases
from external_transaction_mv et
where
et.cashier_name in ('FACEBOOK', 'GoogleCheckout', 'PayPal', 'PayPal-WPP', 'Paypal', 'iTunes', 'PayPal DG', 'Wirecard', 'Zong', 'WorldPay', 'Amazon')
group by 1, 2, 3, 4;

GRANT SELECT ON player_purchase_daily TO GROUP READ_ONLY;
GRANT ALL ON player_purchase_daily TO GROUP READ_WRITE;
GRANT ALL ON player_purchase_daily TO GROUP SCHEMA_MANAGER;


CREATE or replace VIEW account_activity_view
AS
 SELECT DISTINCT ac.player_id,
    date(ac.audit_ts) AS date,
    gvt.game_type,
    p.account_id,
    acs.platform
   FROM audit_command ac
   JOIN table_definition td ON ac.table_id = td.table_id
   JOIN game_variation_template gvt ON td.game_variation_template_id = gvt.game_variation_template_id
   LEFT JOIN lobby_user p ON ac.player_id = p.player_id
   LEFT JOIN account_session acs ON p.account_id = acs.account_id
  WHERE ac.command_type::text <> 'Leave'::text AND ac.command_type::text <> 'GetStatus'::text;

GRANT SELECT ON account_activity_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON account_activity_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON account_activity_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON account_activity_view TO reporting;

update lobby_user lu
set
  reg_ts= pd.created_ts,
  account_id=pd.account_id
from player_definition pd
where lu.player_id=pd.player_id;


alter table player_referrer rename to player_referrer_backup;

create view player_referrer as
  select player_id, registration_referrer, registration_platform, registration_game_type from lobby_user;

GRANT SELECT ON player_referrer TO read_only;
GRANT SELECT ON player_referrer TO read_write;
GRANT SELECT ON player_referrer TO schema_manager;
GRANT SELECT ON player_referrer TO admin;

update lobby_user lu
set
  registration_referrer = pr.registration_referrer,
  registration_platform=pr.registration_platform,
  registration_game_type=pr.registration_game_type
from player_referrer pr
where lu.player_id=pr.player_id;

alter table player_definition rename to player_definition_backup;

create view player_definition as
  select player_id, reg_ts as created_ts, account_id from lobby_user;

GRANT SELECT ON player_definition TO read_only;
GRANT SELECT ON player_definition TO read_write;
GRANT SELECT ON player_definition TO schema_manager;
GRANT SELECT ON player_definition TO admin;
