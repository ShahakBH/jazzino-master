create table $CONFIGURATION(
    name varchar(255) not null primary key,
    value varchar(255) not null
)#

create table $STATUS(
    name varchar(255) not null primary key,
    value varchar(255) not null
)#

drop table if exists AGG_TRANSACTION_LOG_BY_DATE_GVT_ACC#

create table AGG_TRANSACTION_LOG_BY_DATE_GVT_ACC(
    date date,
    game_variation_template_id int not null,
    account_id int not null,
    total_stake decimal(64, 4) not null,
    total_num_stakes int not null,
    total_return decimal(64, 4) not null,
    total_num_returns int not null,
    total_transactions decimal(64, 4) not null,
    constraint primary key (date, game_variation_template_id, account_id)
)#

drop table if exists AGG_DAILY_PAYOUT_BY_GAME_VARIATION#

create table AGG_DAILY_PAYOUT_BY_GAME_VARIATION(
    date date,
    game_variation_template_id int not null,
    total_stake decimal(64, 4) not null,
    total_num_stakes int not null,
    total_return decimal(64, 4) not null,
    total_num_returns int not null,
    constraint primary key (date, game_variation_template_id)
)#

insert into $CONFIGURATION values ('agg_transaction_log_by_date_gvt_acc_batch_size', 100000)#
insert into $STATUS values ('agg_transaction_log_by_date_gvt_acc_last_transaction_id', 0)#

drop procedure if exists calc_agg_transaction_log_by_date_gvt_acc#
drop procedure if exists calc_agg_transaction_log_by_date_gvt_acc_nb#
drop procedure if exists init_agg_transaction_log_by_date_gvt_acc#
drop procedure if exists calc_agg_daily_payout_by_game_variation#

create procedure calc_agg_transaction_log_by_date_gvt_acc(in transaction_id_from int, in transaction_id_to int)
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
    declare l_agg_transaction_log_by_date_gvt_acc_batch_size int;
    declare l_transaction_id_from int;
    declare l_transaction_id_to int;
    declare l_max_transaction_id int;

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
    declare l_start_transaction_log_id int;
    declare l_current_transaction_log_id int;
    declare l_end_transaction_log_id int;

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

call init_agg_transaction_log_by_date_gvt_acc(curdate() - interval 3 day)#

create procedure calc_agg_daily_payout_by_game_variation(in business_date date)
begin
    insert into AGG_DAILY_PAYOUT_BY_GAME_VARIATION
    select
        business_date,
        game_variation_template_id,
        sum(total_stake),
        sum(total_num_stakes),
        sum(total_return),
        sum(total_num_returns)
    from AGG_TRANSACTION_LOG_BY_DATE_GVT_ACC ts
    where date = business_date
    group by game_variation_template_id;
end#

drop event if exists evt_calc_agg_transaction_log_by_date_gvt_acc_nb#

create event evt_calc_agg_transaction_log_by_date_gvt_acc_nb
on schedule every 1 minute
comment 'Calculate transaction log aggregate by day, game variation template, account.'
do call calc_agg_transaction_log_by_date_gvt_acc_nb()#

drop event if exists evt_calc_agg_daily_payout_by_game_variation#

create event evt_calc_agg_daily_payout_by_game_variation
on schedule every 1 day
starts curdate() + interval 1 day + interval 2 hour
comment 'Calculate daily payout by game variation.'
do call calc_agg_daily_payout_by_game_variation(curdate() - interval 1 day)#

create or replace view rpt_payout_by_game_variation_daily_summary as
select
    date,
    name 'Game Variation',
    total_return / total_stake Payout,
    total_stake 'Total Stake',
    total_num_stakes 'Total Number of Stakes',
    total_return 'Total Return',
    total_num_returns 'Total Number of Returns',
    total_num_returns / total_num_stakes 'Win Ratio',
    total_num_stakes + total_num_returns 'Total Transactions'
from AGG_DAILY_PAYOUT_BY_GAME_VARIATION s, strataprod.GAME_VARIATION_TEMPLATE gvt
where s.game_variation_template_id = gvt.game_variation_template_id#

create or replace view rpt_payout_daily_summary as
select
    date,
    sum(total_return) / sum(total_stake) Payout,
    sum(total_stake) 'Total Stake',
    sum(total_num_stakes) 'Total Number of Stakes',
    sum(total_return) 'Total Return',
    sum(total_num_returns) 'Total Number of Returns',
    sum(total_num_returns) / sum(total_num_stakes) 'Win Ratio',
    sum(total_num_stakes) + sum(total_num_returns) 'Total Transactions'
from AGG_DAILY_PAYOUT_BY_GAME_VARIATION s, strataprod.GAME_VARIATION_TEMPLATE gvt
where s.game_variation_template_id = gvt.game_variation_template_id
group by date#

drop table if exists AGG_TRANSACTION_LOG_BY_DATE_TRANSACTION_TYPE#

create table AGG_TRANSACTION_LOG_BY_DATE_TRANSACTION_TYPE(
    date date not null,
    transaction_type varchar(32) not null,
    total_number_of_transactions int not null,
    total_amount decimal(64, 4) not null,
    constraint primary key (date, transaction_type)
)#

insert into $CONFIGURATION values ('agg_transaction_log_by_date_transaction_type_batch_size', 100000)#
insert into $STATUS values ('agg_transaction_log_by_date_transaction_type_last_transaction_id', 0)#

drop procedure if exists calc_agg_transaction_log_by_date_transaction_type#
drop procedure if exists calc_agg_transaction_log_by_date_transaction_type_nb#
drop procedure if exists init_agg_transaction_log_by_date_transaction_type#

create procedure calc_agg_transaction_log_by_date_transaction_type(in transaction_id_from int, in transaction_id_to int)
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
    declare l_batch_size int;
    declare l_transaction_log_from int;
    declare l_transaction_log_to int;
    declare l_max_transaction_id int;

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
    declare l_start_transaction_log_id int;
    declare l_current_transaction_log_id int;
    declare l_end_transaction_log_id int;

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

call init_agg_transaction_log_by_date_transaction_type(curdate() - interval 3 day)#

drop event if exists evt_calc_agg_transaction_log_by_date_transaction_type_nb#

create event evt_calc_agg_transaction_log_by_date_transaction_type_nb
on schedule every 1 minute
comment 'Calculate transaction log aggregate by transaction type.'
do call calc_agg_transaction_log_by_date_transaction_type_nb()#

create or replace view $max_transaction_id as
select max(transaction_log_id) max_transaction_log_id
from TRANSACTION_LOG#

create or replace view $agg_status as
select
    'transaction log by (date, gvt, account)' aggregate,
    (max_transaction_log_id - cast(s.value as unsigned int)) / cast(c.value as unsigned int) pending_batches,
    max_transaction_log_id - cast(s.value as unsigned int) pending_records
from $STATUS s, $CONFIGURATION c, $max_transaction_id
where s.name = 'agg_transaction_log_by_date_gvt_acc_last_transaction_id'
and c.name = 'agg_transaction_log_by_date_gvt_acc_batch_size'
union
select
    'transaction log by (date, transaction_type)' aggregate,
    (max_transaction_log_id - cast(s.value as unsigned int)) / cast(c.value as unsigned int) pending_batches,
    max_transaction_log_id - cast(s.value as unsigned int) pending_records
from $STATUS s, $CONFIGURATION c, $max_transaction_id
where s.name = 'agg_transaction_log_by_date_transaction_type_last_transaction_id'
and c.name = 'agg_transaction_log_by_date_transaction_type_batch_size'#
