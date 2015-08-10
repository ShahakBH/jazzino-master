DROP PROCEDURE IF EXISTS rptExtractPromotionSummary#
create procedure rptExtractPromotionSummary()
begin

  start transaction;

    /* Transactions up to 21 days prior to Promotion start */
    insert into rpt_promotion_transaction (EXTERNAL_TRANSACTION_ID, PROMO_ID, PLAYER_ID, ACCOUNT_ID, CONTROL_GROUP, PAYMENT_TIMESTAMP, AMOUNT, CURRENCY_CODE, AMOUNT_CHIPS)
    (
      select
      x.EXTERNAL_TRANSACTION_ID as EXTERNAL_TRANSACTION_ID, pp.PROMO_ID as promo_id, pl.PLAYER_ID as PLAYER_ID, pl.ACCOUNT_ID as ACCOUNT_ID,
      isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE) as CG,
      x.MESSAGE_TIMESTAMP as ts, x.AMOUNT as amount, x.CURRENCY_CODE as CURRENCY_CODE, x.AMOUNT_CHIPS as AMOUNT_CHIPS

      FROM strataproddw.EXTERNAL_TRANSACTION x
      JOIN strataprod.PLAYER pl ON x.ACCOUNT_ID = pl.ACCOUNT_ID
      JOIN strataprod.LOBBY_USER l ON pl.PLAYER_ID = l.PLAYER_ID
      INNER JOIN strataprod.PROMOTION_PLAYER pp on pl.PLAYER_ID = pp.PLAYER_ID
      INNER JOIN strataprod.PROMOTION pr on pp.PROMO_ID =  pr.PROMO_ID

      WHERE
      AMOUNT > 0 and
      EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND
      x.MESSAGE_TIMESTAMP < pr.start_date and x.MESSAGE_TIMESTAMP > DATE_ADD(pr.start_date, INTERVAL -21 DAY) AND
      x.MESSAGE_TIMESTAMP < CURDATE() AND
      pr.START_DATE >= '2011-12-09' AND pr.END_DATE < CURRENT_TIMESTAMP and TYPE='BUY_CHIPS' and ALL_PLAYERS = 0 and pr.PROMO_ID NOT IN (SELECT PROMO_ID FROM strataproddw.rpt_promotion_summary)
    );

    /* Transactions during the Promotion */
    insert into rpt_promotion_transaction (EXTERNAL_TRANSACTION_ID, PROMO_ID, PLAYER_ID, ACCOUNT_ID, CONTROL_GROUP, PAYMENT_TIMESTAMP, AMOUNT, CURRENCY_CODE, AMOUNT_CHIPS)
    (
      select
      x.EXTERNAL_TRANSACTION_ID as EXTERNAL_TRANSACTION_ID, pp.PROMO_ID as promo_id, pl.PLAYER_ID as PLAYER_ID, pl.ACCOUNT_ID as ACCOUNT_ID,
      isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE) as CG,
      x.MESSAGE_TIMESTAMP as ts, x.AMOUNT as amount, x.CURRENCY_CODE as CURRENCY_CODE, x.AMOUNT_CHIPS as AMOUNT_CHIPS

      FROM strataproddw.EXTERNAL_TRANSACTION x
      JOIN strataprod.PLAYER pl ON x.ACCOUNT_ID = pl.ACCOUNT_ID
      JOIN strataprod.LOBBY_USER l ON pl.PLAYER_ID = l.PLAYER_ID
      INNER JOIN strataprod.PROMOTION_PLAYER pp on pl.PLAYER_ID = pp.PLAYER_ID
      INNER JOIN strataprod.PROMOTION_PLAYER_REWARD ppr on pp.PLAYER_ID = ppr.PLAYER_ID and x.MESSAGE_TIMESTAMP = ppr.REWARDED_DATE
      INNER JOIN strataprod.PROMOTION pr on pp.PROMO_ID =  pr.PROMO_ID

      WHERE
      AMOUNT > 0 AND
      (
        (EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND ppr.DETAILS like 'method=PAYPAL%')
        or
        (EXTERNAL_TRANSACTION_STATUS='REQUEST' AND ppr.DETAILS like 'method=CREDITCARD%')
      ) AND
      pr.ALL_PLAYERS = 0 AND
      x.MESSAGE_TIMESTAMP > pr.start_date and x.MESSAGE_TIMESTAMP <= pr.end_date AND
      pr.START_DATE >= '2011-12-09' AND pr.END_DATE < CURRENT_TIMESTAMP and TYPE='BUY_CHIPS' and ALL_PLAYERS = 0 and pr.PROMO_ID NOT IN (SELECT PROMO_ID FROM strataproddw.rpt_promotion_summary)
    );

    insert into rpt_promotion_summary(
      select pr.PROMO_ID, TYPE, concat(pr.PROMO_ID, '-', name), ALL_PLAYERS, START_DATE, END_DATE, COALESCE(target_count,0), COALESCE(control_group_count,0) from strataprod.PROMOTION pr
      left join
      (
        select
        pp.PROMO_ID , count(*) as target_count
        FROM strataprod.PROMOTION_PLAYER pp
        INNER JOIN strataprod.PROMOTION pr on pp.PROMO_ID =  pr.PROMO_ID
        INNER JOIN strataprod.PLAYER pl on pp.PLAYER_ID =  pl.PLAYER_ID
        INNER JOIN strataprod.LOBBY_USER l ON pl.PLAYER_ID = l.PLAYER_ID

        WHERE isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE) = 0
        group by pp.promo_ID, isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE)

      ) target on pr.PROMO_ID = target.PROMO_ID
      left join
      (
        select
        pp.PROMO_ID , count(*) as control_group_count
        FROM strataprod.PROMOTION_PLAYER pp
        INNER JOIN strataprod.PROMOTION pr on pp.PROMO_ID =  pr.PROMO_ID
        INNER JOIN strataprod.PLAYER pl on pp.PLAYER_ID =  pl.PLAYER_ID
        INNER JOIN strataprod.LOBBY_USER l ON pl.PLAYER_ID = l.PLAYER_ID

        WHERE isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE) = 1
        group by pp.promo_ID, isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE)

      ) control_group on pr.PROMO_ID = control_group.PROMO_ID

      where pr.START_DATE >= '2011-12-09' AND pr.END_DATE < CURRENT_TIMESTAMP and TYPE='BUY_CHIPS' and ALL_PLAYERS = 0 and pr.PROMO_ID NOT IN (SELECT PROMO_ID FROM strataproddw.rpt_promotion_summary)
      group by pr.PROMO_ID
    );

  commit;

END;
#


DROP PROCEDURE IF EXISTS rptExtractPromotionTransactionsAfterExpiry#
create procedure rptExtractPromotionTransactionsAfterExpiry()
begin

  declare lastInsertDate date default null;

  select ifnull(date(action_ts), DATE_ADD(CURDATE(), INTERVAL -1 DAY)) into @lastInsertDate from rpt_report_status where report_action = 'promotionTransactionsExtractLastRun';

  start transaction;

    insert into rpt_promotion_transaction (EXTERNAL_TRANSACTION_ID, PROMO_ID, PLAYER_ID, ACCOUNT_ID, CONTROL_GROUP, PAYMENT_TIMESTAMP, AMOUNT, CURRENCY_CODE, AMOUNT_CHIPS)
    (
      select
      x.EXTERNAL_TRANSACTION_ID as EXTERNAL_TRANSACTION_ID, pp.PROMO_ID as promo_id, pl.PLAYER_ID as PLAYER_ID, pl.ACCOUNT_ID as ACCOUNT_ID,
      isMemberOfControlGroup(pr.CG_FUNCTION, l.PROVIDER_NAME, pl.EXTERNAL_ID, pl.PLAYER_ID, l.USER_ID, pr.SEED, pr.CONTROL_GROUP_PERCENTAGE) as CG,

      x.MESSAGE_TIMESTAMP as ts, x.AMOUNT as amount, x.CURRENCY_CODE as CURRENCY_CODE, x.AMOUNT_CHIPS as AMOUNT_CHIPS

      FROM strataproddw.EXTERNAL_TRANSACTION x
      JOIN strataprod.PLAYER pl ON x.ACCOUNT_ID = pl.ACCOUNT_ID
      JOIN strataprod.LOBBY_USER l ON pl.PLAYER_ID = l.PLAYER_ID
      INNER JOIN strataprod.PROMOTION_PLAYER pp on pl.PLAYER_ID = pp.PLAYER_ID
      INNER JOIN strataprod.PROMOTION pr on pp.PROMO_ID =  pr.PROMO_ID

      WHERE
      AMOUNT > 0 and
      EXTERNAL_TRANSACTION_STATUS='SUCCESS' AND
      x.MESSAGE_TIMESTAMP > pr.end_date AND x.MESSAGE_TIMESTAMP < DATE_ADD(pr.end_date, INTERVAL 21 DAY) AND
      x.MESSAGE_TIMESTAMP >= @lastInsertDate AND
      x.MESSAGE_TIMESTAMP < CURDATE() AND
      pr.START_DATE >= '2011-12-09' AND
      TYPE='BUY_CHIPS'
    );

    UPDATE rpt_report_status set action_ts = now() where report_action = 'promotionTransactionsExtractLastRun';

  commit;

end;
#
