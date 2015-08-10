drop procedure if exists calc_agg_transaction_log_by_date_gvt_acc#

create procedure calc_agg_transaction_log_by_date_gvt_acc(in transaction_id_from bigint, in transaction_id_to bigint)
begin
    insert into AGG_TRANSACTION_LOG_BY_DATE_GVT_ACC
        select
            date(transaction_ts) date,
            game_variation_template_id,
            account_id,
            sum(if(amount < 0, -amount, 0))  total_stake,
            sum(if(amount < 0, 1, 0)) total_num_stakes,
            sum(if(amount > 0, amount, 0)) total_return,
            sum(if(amount > 0, 1, 0)) total_num_returns,
            count(transaction_log_id) transactions
        from TRANSACTION_LOG tl, strataprod.TABLE_INFO ti
        where substring_index(reference, '|', 1) = table_id
        and transaction_log_id > transaction_id_from
        and transaction_log_id <= transaction_id_to
        and transaction_type in ('Stake', 'Return')
        and amount <> 0
        group by date(transaction_ts), game_variation_template_id, account_id
    on duplicate key update
        total_stake = total_stake + values(total_stake),
        total_num_stakes = total_num_stakes + values(total_num_stakes),
        total_return = total_return + values(total_return),
        total_num_returns = total_num_returns + values(total_num_returns),
        total_transactions = total_transactions + values(total_transactions);
end#
