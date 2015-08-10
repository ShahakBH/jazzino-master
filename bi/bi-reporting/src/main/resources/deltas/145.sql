-- Added view for new vs existing users via ducksboard

CREATE OR REPLACE VIEW players_new_vs_existing_view AS
  SELECT date_trunc('day',activity_ts) AS audit_date,
         CASE WHEN date_trunc('day', activity_ts) = date_trunc('day', reg_ts) THEN 'new' ELSE 'existing' END AS player_type,
         count(DISTINCT player_id)
  FROM player_activity_daily
  WHERE activity_ts >= date_trunc('day',now()) - interval '1 day'
  GROUP BY 1, 2;

GRANT SELECT ON players_new_vs_existing_view TO GROUP READ_ONLY;
GRANT ALL ON players_new_vs_existing_view TO GROUP READ_WRITE;
GRANT ALL ON players_new_vs_existing_view TO GROUP SCHEMA_MANAGER;
