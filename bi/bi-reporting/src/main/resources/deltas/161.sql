-- drop views that depend on external_transaction_mv

DROP VIEW IF EXISTS ad_tracking_view;
DROP VIEW IF EXISTS purchase_with_offers_view;
DROP VIEW IF EXISTS purchase_view;
DROP VIEW IF EXISTS player_purchase_daily;
DROP MATERIALIZED VIEW IF EXISTS lifetime_value_buyers_mv;
DROP MATERIALIZED VIEW IF EXISTS player_purchase_daily_by_game;

-- recreate external_transaction_mv adding partner_id

DROP MATERIALIZED VIEW external_transaction_mv;
CREATE MATERIALIZED VIEW external_transaction_mv
AS
  SELECT
    et.player_id,
    lu.account_id,
    lu.reg_ts::date AS registration_date,
    lu.registration_platform,
    lu.registration_referrer,
    lu.partner_id,
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

ALTER TABLE external_transaction_mv owner to SCHEMA_MANAGER;

-- recreate ad_tracking_view (from 148.sql)

CREATE VIEW ad_tracking_view AS
  SELECT
    lu.player_id,
    lu.reg_ts::date registration_date,
    lu.registration_platform,
    lu.registration_referrer,
    et.purchase_ts,
    et.account_id,
    et.amount,
    et.currency_code,
    et.amount_gbp,
    et.cashier_name,
    et.amount_chips,
    et.game_type,
    et.purchase_platform
  FROM lobby_user lu
    LEFT JOIN external_transaction_mv et
      ON lu.account_id = et.account_id;

GRANT SELECT ON ad_tracking_view TO GROUP READ_ONLY;
GRANT ALL ON ad_tracking_view TO GROUP READ_WRITE;
GRANT ALL ON ad_tracking_view TO GROUP SCHEMA_MANAGER;

-- recreate purchase_with_offers_view (from 143.sql)

CREATE VIEW purchase_with_offers_view AS SELECT * FROM external_transaction_mv;

GRANT SELECT ON purchase_with_offers_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON purchase_with_offers_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON purchase_with_offers_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON purchase_with_offers_view TO reporting;

-- recreate purchase_view (from 143.sql but using the cashiers table)

CREATE VIEW purchase_view AS
  SELECT * FROM external_transaction_mv et
  WHERE et.cashier_name IN (SELECT cashier_name FROM cashiers WHERE is_purchase = true);

GRANT SELECT ON purchase_view TO read_only;
GRANT SELECT, UPDATE, INSERT, DELETE, REFERENCES, TRIGGER, TRUNCATE ON purchase_view TO read_write;
GRANT UPDATE, DELETE, TRUNCATE, TRIGGER, SELECT, REFERENCES, INSERT ON purchase_view TO schema_manager;
GRANT INSERT, SELECT, TRUNCATE, REFERENCES, UPDATE, TRIGGER, DELETE ON purchase_view TO reporting;

-- recreate player_purchase_daily (from 143.sql but using the cashiers table)

CREATE VIEW player_purchase_daily AS
  SELECT
    player_id,
    registration_date,
    purchase_platform,
    purchase_ts::date purchase_date,
    sum(amount_gbp) total_amount_gbp,
    count(1) num_purchases
  FROM external_transaction_mv et
  WHERE et.cashier_name IN (SELECT cashier_name FROM cashiers WHERE is_purchase = true)
  GROUP BY 1, 2, 3, 4;

GRANT SELECT ON player_purchase_daily TO GROUP READ_ONLY;
GRANT ALL ON player_purchase_daily TO GROUP READ_WRITE;
GRANT ALL ON player_purchase_daily TO GROUP SCHEMA_MANAGER;

-- recreate lifetime_value_buyers_mv (from 152.sql)

CREATE MATERIALIZED VIEW lifetime_value_buyers_mv AS
  SELECT
    a.purchase_platform,
    a.first_purchase_month,
    a.purchase_month,
    a.total_amount_gbp,
    b.num_buyers
  FROM (SELECT
          purchase_platform,
          to_char(first_purchase_date, 'YYYY-MM') first_purchase_month,
          to_char(purchase_ts, 'YYYY-MM') purchase_month,
          sum(amount_gbp) total_amount_gbp,
          count(distinct player_id) num_buyers
        FROM public.external_transaction_mv
          JOIN cashiers ON cashiers.cashier_name = external_transaction_mv.cashier_name
        WHERE is_purchase =true
        GROUP BY 1, 2, 3) a, (SELECT
                                purchase_platform,
                                to_char(first_purchase_date, 'YYYY-MM') first_purchase_month,
                                count(distinct player_id) num_buyers
                              FROM public.external_transaction_mv
                                JOIN cashiers ON cashiers.cashier_name = external_transaction_mv.cashier_name
                              WHERE is_purchase =true
                              GROUP BY 1, 2) b
  WHERE a.purchase_platform = b.purchase_platform
        AND a.first_purchase_month = b.first_purchase_month;

GRANT SELECT ON lifetime_value_buyers_mv TO GROUP read_only;
GRANT ALL ON lifetime_value_buyers_mv TO GROUP read_write;
GRANT ALL ON lifetime_value_buyers_mv TO GROUP schema_manager;

-- recreate player_purchase_daily_by_game (from 155.sql but using the cashiers table)

CREATE MATERIALIZED VIEW player_purchase_daily_by_game AS
  SELECT et.player_id,
    et.registration_date,
    et.game_type game,
    et.purchase_platform,
    et.purchase_ts::date AS purchase_date,
    sum(et.amount_gbp) AS total_amount_gbp,
    count(1) AS num_purchases
  FROM external_transaction_mv et
  WHERE et.cashier_name IN (SELECT cashier_name FROM cashiers WHERE is_purchase = true)
  GROUP BY 1, 2, 3, 4, 5;

CREATE UNIQUE INDEX ON player_purchase_daily_by_game (player_id, purchase_date, purchase_platform, game);
