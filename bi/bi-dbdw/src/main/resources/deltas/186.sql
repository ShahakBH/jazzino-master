drop event if exists evt_recreate_promotion_reports#

drop event if exists rptExtractPromotionSummaryDaily#

drop procedure if exists recreate_promotion_reports#

drop procedure if exists rptExtractPromotionSummary#

drop table if exists rpt_promotion_uptake#

drop table if exists rpt_promotion_uptake_daily#

drop table if exists rpt_promotion_transaction#

drop table if exists rpt_promotion_summary#

drop function if exists extractRevenueAttributableToPromotion#

drop function if exists inferRevenueFromDefaultChips#
