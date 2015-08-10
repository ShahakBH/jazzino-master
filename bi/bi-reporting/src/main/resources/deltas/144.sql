-- Add view for daily purchases by platform

CREATE OR REPLACE VIEW purchases_by_platform_last_day_view AS
  SELECT et.platform,
         ROUND(COALESCE(sum(et.amount / cr.rate), 0::numeric), 2) AS total_amount
  FROM external_transaction et
  JOIN currency_rates cr ON et.currency_code::text = cr.currency_code::text
  WHERE et.message_ts > (now() - interval '1 day')
    AND (et.external_transaction_status::text = 'SUCCESS'::text OR et.external_transaction_status::text = 'AUTHORISED'::text)
  GROUP BY et.platform;

GRANT SELECT ON purchases_by_platform_last_day_view TO GROUP READ_ONLY;
GRANT ALL ON purchases_by_platform_last_day_view TO GROUP READ_WRITE;
GRANT ALL ON purchases_by_platform_last_day_view TO GROUP SCHEMA_MANAGER;
