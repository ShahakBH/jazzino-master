drop table if exists view_promotion_union#

drop table if exists view_promotion_config_union#

drop table if exists view_promotion_player_union#

drop table if exists view_promotion_player_reward_union#

drop procedure if exists recreate_promotion_reports#

create procedure recreate_promotion_reports()
begin
    delete from rpt_promotion_uptake
    where promo_id in (select promo_id from strataprod.PROMOTION);

    insert into rpt_promotion_uptake
    select
        a.promo_id,
        a.control_group,
        promotion_type,
        start_date,
        end_date,
        unique_users_target,
        total_taken,
        unique_takers,
        chips_issued,
        total_revenue
    from (
        select
            pr.promo_id,
            isMemberOfControlGroup(cg_function, provider_name, pl.external_id, pl.player_id, pl.user_id, seed, control_group_percentage) control_group,
            pr.type promotion_type,
            start_date,
            end_date,
            count(1) unique_users_target
        from strataprod.PROMOTION pr, strataprod.PROMOTION_PLAYER pp, strataprod.PLAYER pl, strataprod.LOBBY_USER lu
        where pr.promo_id = pp.promo_id
        and pp.player_id = pl.player_id
        and pl.player_id = lu.player_id
        and type = 'BUY_CHIPS'
        and all_players = 0
        group by promo_id, control_group) a left join (
        select
            pr.promo_id,
            control_group,
            count(1) total_taken,
            count(distinct player_id) unique_takers,
            null chips_issued,
            ifnull(sum(strataproddw.extractRevenueAttributableToPromotion(details)), 0) total_revenue
        from strataprod.PROMOTION pr, strataprod.PROMOTION_PLAYER_REWARD ppr
        where pr.promo_id = ppr.promo_id
        and type = 'BUY_CHIPS'
        group by pr.promo_id, control_group) b
    on a.promo_id = b.promo_id
    and a.control_group = b.control_group;

    insert into rpt_promotion_uptake
        select
                promo_id,
                control_group,
                TYPE as promotion_type,
                start_date,
                end_date,
                (select count(1) from strataprod.PROMOTION_PLAYER where promo_id = p.promo_id), /*unique_users_target,*/
                TOTAL_TAKEN,
                DISTINCT_TOTAL,
                TOTAL_TAKEN * CONFIG.REWARD,
                null
            from strataprod.PROMOTION p
      INNER JOIN (    SELECT PROMO_ID, CONFIG_KEY, CONVERT(CONFIG_VALUE, SIGNED) AS REWARD
                        FROM strataprod.PROMOTION_CONFIG
                       WHERE CONFIG_KEY = 'reward.chips'    ) AS CONFIG
           USING (PROMO_ID)
            JOIN (    SELECT PROMO_ID, CONTROL_GROUP, COUNT(DISTINCT PLAYER_ID) AS DISTINCT_TOTAL, COUNT(*) AS TOTAL_TAKEN
                        FROM strataprod.PROMOTION_PLAYER_REWARD
                    GROUP BY PROMO_ID, CONTROL_GROUP    ) AS SOURCE

           USING (PROMO_ID)
             WHERE TYPE = 'DAILY_AWARD'
             and ALL_PLAYERS = 0;

    insert ignore into rpt_promotion_uptake
        select
            promo_id,
            control_group,
            type promotion_type,
            start_date,
            end_date,
            0,
            0,
            0,
            if(pr.type = 'DAILY_AWARD', 0, null),
            if(pr.type = 'BUY_CHIPS', 0, null)
    from strataprod.PROMOTION pr, (select 0 control_group union select 1) cg
    where all_players = 0;
/*
    select
        pc.promo_id,
        control_group,
        type promotion_type,
        start_date,
        end_date,
        null unique_users_target,
        count(1) total_taken,
        count(distinct player_id) unique_takers,
        count(1) * convert(config_value, signed) chips_issued,
        null total_revenue
    from strataprod.PROMOTION_CONFIG pc, strataprod.PROMOTION pr, strataprod.PROMOTION_PLAYER_REWARD ppr
    where pc.promo_id = ppr.promo_id
    and pc.promo_id = pr.promo_id
    and config_key = 'reward.chips'
    group by ppr.promo_id, control_group;

    select
        a.promo_id,
        a.control_group,
        promotion_type,
        start_date,
        end_date,
        unique_users_target,
        total_taken,
        unique_takers,
        chips_issued,
        total_revenue
    from (
        select
            pr.promo_id,
            isMemberOfControlGroup(cg_function, provider_name, pl.external_id, pl.player_id, pl.user_id, seed, control_group_percentage) control_group,
            pr.type promotion_type,
            start_date,
            end_date,
            count(1) unique_users_target
        from strataproddw.PROMOTION_ARCHIVE pr, strataproddw.PROMOTION_PLAYER_ARCHIVE pp, strataprod.PLAYER pl, strataprod.LOBBY_USER lu
        where pr.promo_id = pp.promo_id
        and pp.player_id = pl.player_id
        and pl.player_id = lu.player_id
        and type = 'BUY_CHIPS'
        group by promo_id, control_group) a left join (
        select
            pr.promo_id,
            control_group,
            count(1) total_taken,
            count(distinct player_id) unique_takers,
            null chips_issued,
            sum(strataproddw.extractRevenueAttributableToPromotion(details)) total_revenue
        from strataproddw.PROMOTION_ARCHIVE pr, strataproddw.PROMOTION_PLAYER_REWARD_ARCHIVE ppr
        where pr.promo_id = ppr.promo_id
        and type = 'BUY_CHIPS'
        group by pr.promo_id, control_group) b
    on a.promo_id = b.promo_id
    and a.control_group = b.control_group;
*/
    delete from rpt_promotion_uptake_daily
    where promo_id in (select promo_id from strataprod.PROMOTION);

    insert into rpt_promotion_uptake_daily (promo_id, control_group, day_of_promotion, total_taken)
    select
        p.promo_id,
        control_group,
        1 + floor(((to_seconds(rewarded_date) - to_seconds(start_date)) * 1.0) / (60 * 60 * 24.0)) as day_of_promotion,
        count(distinct player_id) as total_taken
    from strataprod.PROMOTION_PLAYER_REWARD ppr, strataprod.PROMOTION p
    where ppr.promo_id =  p.promo_id
    group by promo_id, control_group, day_of_promotion
    having day_of_promotion >= 1 and day_of_promotion <= 7;
end#
