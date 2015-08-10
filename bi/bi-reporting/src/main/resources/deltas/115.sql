alter table dmr_player_activity_and_purchases rename column registration_adnet2 to registration_adnet;
--this is to fix the incorrectly named column. it shouldn't be run on prod as it's not broken on prod.
-- if it is run on prod then simply move up the changelog up.
