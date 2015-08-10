drop procedure if exists calc_agg_transaction_log_by_date_gvt_acc#
drop procedure if exists calc_agg_transaction_log_by_date_gvt_acc_nb#
drop procedure if exists init_agg_transaction_log_by_date_gvt_acc#

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
        group by date(transaction_ts), game_variation_template_id, account_id
    on duplicate key update
        total_stake = total_stake + values(total_stake),
        total_num_stakes = total_num_stakes + values(total_num_stakes),
        total_return = total_return + values(total_return),
        total_num_returns = total_num_returns + values(total_num_returns),
        total_transactions = total_transactions + values(total_transactions);
end#

create procedure calc_agg_transaction_log_by_date_gvt_acc_nb()
begin
    declare l_agg_transaction_log_by_date_gvt_acc_batch_size bigint;
    declare l_transaction_id_from bigint;
    declare l_transaction_id_to bigint;
    declare l_max_transaction_id bigint;

    if get_lock('strataproddw.calc_agg_transaction_log_by_date_gvt_acc_nb', 0) = 1 then
        select cast(value as unsigned int)
        into l_agg_transaction_log_by_date_gvt_acc_batch_size
        from $CONFIGURATION
        where name = 'agg_transaction_log_by_date_gvt_acc_batch_size';

        select cast(value as unsigned int)
        into l_transaction_id_from
        from $STATUS
        where name = 'agg_transaction_log_by_date_gvt_acc_last_transaction_id';

        select coalesce(max(transaction_log_id), 0)
        into l_max_transaction_id
        from TRANSACTION_LOG;

        if l_transaction_id_from + l_agg_transaction_log_by_date_gvt_acc_batch_size <= l_max_transaction_id then
            set l_transaction_id_to = l_transaction_id_from + l_agg_transaction_log_by_date_gvt_acc_batch_size;
        else
            set l_transaction_id_to = l_max_transaction_id;
        end if;

        call calc_agg_transaction_log_by_date_gvt_acc(l_transaction_id_from, l_transaction_id_to);

        update $STATUS
        set value = l_transaction_id_to
        where name = 'agg_transaction_log_by_date_gvt_acc_last_transaction_id';

        do release_lock('strataproddw.calc_agg_transaction_log_by_date_gvt_acc_nb');
    end if;
end#

create procedure init_agg_transaction_log_by_date_gvt_acc(in date_from date)
begin
    declare l_start_transaction_log_id bigint;
    declare l_current_transaction_log_id bigint;
    declare l_end_transaction_log_id bigint;

    select coalesce(min(transaction_log_id), 0)
    into l_start_transaction_log_id
    from TRANSACTION_LOG
    where transaction_ts >= date_from;

    update $STATUS
    set value = l_start_transaction_log_id
    where name = 'agg_transaction_log_by_date_gvt_acc_last_transaction_id';

    select coalesce(max(transaction_log_id), 0)
    into l_end_transaction_log_id
    from TRANSACTION_LOG;

    repeat
        start transaction;
        call calc_agg_transaction_log_by_date_gvt_acc_nb();
        commit;
        select cast(value as unsigned int)
        into l_current_transaction_log_id
        from $STATUS
        where name = 'agg_transaction_log_by_date_gvt_acc_last_transaction_id';
    until l_current_transaction_log_id >= l_end_transaction_log_id end repeat;
end#

drop procedure if exists calc_agg_transaction_log_by_date_transaction_type#
drop procedure if exists calc_agg_transaction_log_by_date_transaction_type_nb#
drop procedure if exists init_agg_transaction_log_by_date_transaction_type#

create procedure calc_agg_transaction_log_by_date_transaction_type(in transaction_id_from bigint, in transaction_id_to bigint)
begin
    insert into AGG_TRANSACTION_LOG_BY_DATE_TRANSACTION_TYPE
        select
            date(transaction_ts),
            transaction_type,
            count(transaction_log_id),
            sum(amount)
        from TRANSACTION_LOG tl
        where transaction_log_id > transaction_id_from
        and transaction_log_id <= transaction_id_to
        group by date(transaction_ts), transaction_type
    on duplicate key update
        total_number_of_transactions = total_number_of_transactions + values(total_number_of_transactions),
        total_amount = total_amount + values(total_amount);
end#

create procedure calc_agg_transaction_log_by_date_transaction_type_nb()
begin
    declare l_batch_size bigint;
    declare l_transaction_log_from bigint;
    declare l_transaction_log_to bigint;
    declare l_max_transaction_id bigint;

    if get_lock('strataproddw.calc_agg_transaction_log_by_date_transaction_type_nb', 0) = 1 then
        select cast(value as unsigned int)
        into l_batch_size
        from $CONFIGURATION
        where name = 'agg_transaction_log_by_date_transaction_type_batch_size';

        select cast(value as unsigned int)
        into l_transaction_log_from
        from $STATUS
        where name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id';

        select coalesce(max(transaction_log_id), 0)
        into l_max_transaction_id
        from TRANSACTION_LOG;

        if l_transaction_log_from + l_batch_size <= l_max_transaction_id then
            set l_transaction_log_to = l_transaction_log_from + l_batch_size;
        else
            set l_transaction_log_to = l_max_transaction_id;
        end if;

        call calc_agg_transaction_log_by_date_transaction_type(l_transaction_log_from, l_transaction_log_to);

        update $STATUS
        set value = l_transaction_log_to
        where name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id';

        do release_lock('strataproddw.calc_agg_transaction_log_by_date_transaction_type_nb');
    end if;
end#

create procedure init_agg_transaction_log_by_date_transaction_type(in date_from date)
begin
    declare l_start_transaction_log_id bigint;
    declare l_current_transaction_log_id bigint;
    declare l_end_transaction_log_id bigint;

    select coalesce(min(transaction_log_id), 0)
    into l_start_transaction_log_id
    from TRANSACTION_LOG
    where transaction_ts >= date_from;

    update $STATUS
    set value = l_start_transaction_log_id
    where name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id';

    select coalesce(max(transaction_log_id), 0)
    into l_end_transaction_log_id
    from TRANSACTION_LOG;

    truncate table AGG_TRANSACTION_LOG_BY_DATE_TRANSACTION_TYPE;

    repeat
        start transaction;
        call calc_agg_transaction_log_by_date_transaction_type_nb();
        commit;
        select cast(value as unsigned int)
        into l_current_transaction_log_id
        from $STATUS
        where name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id';
    until l_current_transaction_log_id >= l_end_transaction_log_id end repeat;
end#
