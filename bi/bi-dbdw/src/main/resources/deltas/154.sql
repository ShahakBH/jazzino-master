drop procedure if exists setup_progressive_bonus_reminder_app_request#

create procedure setup_progressive_bonus_reminder_app_request()
begin
    drop table if exists tmp_app_request_target;

    create temporary table tmp_app_request_target as
    select distinct
        player_id,
        game_type
    from strataproddw.rpt_account_activity
    where audit_date = curdate() - interval 1 day
    and game_type <> '';

    alter table tmp_app_request_target add primary key (player_id, game_type);

    drop table if exists tmp_app_request_template;

    create temporary table tmp_app_request_template (
        game_type varchar(32) not null,
        day_of_week int not null,
        message varchar(100) not null,
        primary key (game_type, day_of_week)
    );

    insert into tmp_app_request_template values
    ('SLOTS', 1, 'Race to the reels now to collect your FREE DAILY BONUS. Start spinning now!'),
    ('SLOTS', 2, 'Earn FREE BONUS CHIPS every day you play at the reels. Join a game!'),
    ('SLOTS', 3, 'Get even more FREE BONUS CHIPS. Just play Wheel Deal now to collect them!'),
    ('SLOTS', 4, 'Play Wheel Deal NOW to collect your FREE DAILY BONUS. Race to the reels!'),
    ('SLOTS', 5, 'Your reel buddies are waiting for you! Play now to get even more FREE BONUS CHIPS.'),
    ('SLOTS', 6, 'Start spinning and increase your DAILY BONUS CHIPS every day you play. Go NOW!'),
    ('SLOTS', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Spin your way to huge winnings today!'),
    ('BLACKJACK', 1, 'Play Blackjack now to collect your FREE DAILY BONUS. Try your hand now!'),
    ('BLACKJACK', 2, 'Earn FREE BONUS CHIPS when you play Blackjack. Can you beat the dealer? Play now!'),
    ('BLACKJACK', 3, 'Place your bets now to get even more FREE BONUS CHIPS. Join a game!'),
    ('BLACKJACK', 4, 'Be the best in Blackjack with extra chips each day you play! Collect your FREE DAILY BONUS now.'),
    ('BLACKJACK', 5, 'Beat your buddies in Blackjack with FREE BONUS CHIPS when you play now!'),
    ('BLACKJACK', 6, 'Take a seat and try your luck at the table with even more chips. Get your FREE DAILY BONUS now!'),
    ('BLACKJACK', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Can you get the highest hand? Go play!'),
    ('TEXAS_HOLDEM', 1, 'Turn up the fun in Texas Hold’em with DAILY BONUS CHIPS! Play now to collect yours.'),
    ('TEXAS_HOLDEM', 2, 'Boost your balance when you play every day. Collect your FREE BONUS CHIPS and try your hand now!'),
    ('TEXAS_HOLDEM', 3, 'Collect your FREE DAILY BONUS and take a seat at a Table. Play Texas Hold’em now!'),
    ('TEXAS_HOLDEM', 4, 'Grab your FREE BONUS CHIPS and see if you can be crowned the weekly champ! Play now.'),
    ('TEXAS_HOLDEM', 5, 'Take a seat and try your luck at the table with even more chips. Get your FREE DAILY BONUS now!'),
    ('TEXAS_HOLDEM', 6, 'Get a FREE DAILY BONUS when you play every day! Join a tournament and collect yours now.'),
    ('TEXAS_HOLDEM', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Try your hand at the tables now!'),
    ('BINGO', 1, 'Be the best on the Extreme Bingo battleboard with DAILY BONUS CHIPS. Collect yours now!'),
    ('BINGO', 2, 'Boost your balance when you play every day. Get fired up and collect your FREE BONUS CHIPS.'),
    ('BINGO', 3, 'Get even more FREE BONUS CHIPS every day you play. Bolt to the battleboard now!'),
    ('BINGO', 4, 'Earn FREE BONUS CHIPS when you play Extreme Bingo. Want to be the best? Play now!'),
    ('BINGO', 5, 'Bolt to the battleboard to collect your FREE DAILY BONUS now!'),
    ('BINGO', 6, 'Have a ball! Fire up a game with your FREE DAILY BONUS CHIPS now.'),
    ('BINGO', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Boost your balance and play Extreme Bingo now!'),
    ('HIGH_STAKES', 1, 'Grab your FREE DAILY BONUS and see if you can crack the safe! Play High Stakes now.'),
    ('HIGH_STAKES', 2, 'Earn FREE BONUS CHIPS every day you play at the reels. Want to be the boss? Join a game now!'),
    ('HIGH_STAKES', 3, 'Boost your balance when you play every day. Grab your crew and collect FREE BONUS CHIPS now!'),
    ('HIGH_STAKES', 4, 'Spin the reels and collect your DAILY BONUS CHIPS now. Can you win the gem?'),
    ('HIGH_STAKES', 5, 'Raise your FREE BONUS CHIPS every day you play. Start earning your promotion!'),
    ('HIGH_STAKES', 6, 'Increase your DAILY BONUS CHIPS every day you play. Can you pull off a heist? Start spinning now!'),
    ('HIGH_STAKES', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Race to the reels now to win big!'),
    ('HISSTERIA', 1, 'Grab your FREE DAILY BONUS and see if you can reach the top spot. Get it now!'),
    ('HISSTERIA', 2, 'Get even more FREE BONUS CHIPS every day you play. Hurry to Hissteria now!'),
    ('HISSTERIA', 3, 'Slither your way to the jackpot with even more DAILY BONUS CHIPS when you play now.'),
    ('HISSTERIA', 4, 'Boost your balance when you play every day. Are you feeling lucky? Grab your FREE BONUS CHIPS now!'),
    ('HISSTERIA', 5, 'Shake and stir up your game with your DAILY BONUS CHIPS. Go play now!'),
    ('HISSTERIA', 6, 'Beat your buddies to the jackpot now, with even more FREE BONUS CHIPS.'),
    ('HISSTERIA', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Shake up your game!'),
    ('ROULETTE', 1, 'Give the wheel a whirl and collect your FREE DAILY BONUS now!'),
    ('ROULETTE', 2, 'Guess where the ball will land with even more FREE BONUS CHIPS. Go play!'),
    ('ROULETTE', 3, 'Boost your balance when you play every day. Grab your FREE BONUS CHIPS and start spinning!'),
    ('ROULETTE', 4, 'Get even more FREE BONUS CHIPS every day you play. Spin to win now!'),
    ('ROULETTE', 5, 'Collect your FREE DAILY BONUS and place your bets in Roulette now!'),
    ('ROULETTE', 6, 'Spin the wheel and collect your DAILY BONUS CHIPS now. Will you guess where the ball will land?'),
    ('ROULETTE', 7, 'Earn up to 5,000 DAILY BONUS CHIPS just for playing. Give the wheel a whirl!');

    insert into strataproddw.APP_REQUEST (target_client, title, description, message, tracking, created, scheduled_dt, expiry_dt)
    select
        target_client,
        game_type title,
        concat('Progressive Bonus Reminders ', game_type, ' ', curdate()) description,
        message message,
        concat('progressive_bonus_reminder_', day_of_week, '_', game_type) tracking,
        now() created,
        curdate() + interval 5 hour scheduled_dt,
        curdate() + interval 5 hour + interval 1 day expiry_dt
    from tmp_app_request_template t, (select 'FACEBOOK' target_client union select 'IOS' target_client) tc
    where day_of_week = dayofweek(curdate());

    insert into strataproddw.APP_REQUEST_TARGET (app_request_id, player_id, external_id, game_type)
    select
        ar.id,
        mt.player_id,
        lu.external_id,
        mt.game_type
    from tmp_app_request_target mt, strataprod.LOBBY_USER lu, strataproddw.APP_REQUEST ar
    where mt.player_id = lu.player_id
    and ar.description = concat('Progressive Bonus Reminders ', mt.game_type, ' ', curdate())
    and ar.target_client = 'FACEBOOK'
    and provider_name = 'facebook';

    insert into strataproddw.APP_REQUEST_TARGET (app_request_id, player_id, external_id, game_type)
    select distinct
        ar.id,
        mt.player_id,
        null,
        mt.game_type
    from tmp_app_request_target mt, strataprod.IOS_PLAYER_DEVICE d, strataproddw.APP_REQUEST ar
    where mt.player_id = d.player_id
    and mt.game_type = d.game_type
    and ar.description = concat('Progressive Bonus Reminders ', mt.game_type, ' ', curdate())
    and ar.target_client = 'IOS';

    update strataproddw.APP_REQUEST ar, (select
        a.id,
        count(1) target_count
    from strataproddw.APP_REQUEST a, strataproddw.APP_REQUEST_TARGET rt
    where a.id = rt.app_request_id
    and created > curdate()
    group by 1) tc
    set ar.target_count = tc.target_count
    where ar.id = tc.id;

    delete from strataproddw.APP_REQUEST
    where created > curdate()
    and description like 'Progressive Bonus Reminders %'
    and target_count = 0;

    drop table tmp_app_request_target;

    drop table tmp_app_request_template;
end#

drop event if exists evt_setup_progressive_bonus_reminder_app_request#

create event evt_setup_progressive_bonus_reminder_app_request
on schedule every 1 day
starts curdate() + interval 1 day + interval 4 hour
do call setup_progressive_bonus_reminder_app_request()#
