create view rpt_engagement_by_platform as
select
    e1.played_ts,
    e1.game_variation_template_name,
    e1.platform,
    -100 * e2.total_amount / e1.total_amount payout,
    e1.num_players,
    e1.num_transactions num_stakes,
    e1.total_amount total_stakes,
    e2.num_transactions num_returns,
    e2.total_amount total_returns
from engagement_by_platform e1, engagement_by_platform e2
where e1.played_ts = e2.played_ts
and e1.game_variation_template_name = e2.game_variation_template_name
and e1.platform = e2.platform
and e1.transaction_type = 'Stake'
and e2.transaction_type = 'Return'
order by 1, 2, 3;

GRANT SELECT ON rpt_engagement_by_platform TO GROUP READ_ONLY;
GRANT ALL ON rpt_engagement_by_platform TO GROUP READ_WRITE;
GRANT ALL ON rpt_engagement_by_platform TO GROUP SCHEMA_MANAGER;
