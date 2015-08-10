#!/bin/ruby
# This scripts generates n bots for an empty database.
# A gigaspaces redeployment is required after the generated script is executed

@robots = 10000

def create_queries (insert_part, value_part)
  (1..@robots).each do |i|
    if i % 100 == 1
      puts insert_part
    end
    puts value_part.gsub(/ROBOT_ID/, i.to_s)
    if i % 100 == 0
      puts ";"
    else
      puts ","
    end
  end
end

create_queries "INSERT INTO `LOBBY_USER` (`USER_ID`, `EMAIL_ADDRESS`,`UNVERIFIED_EMAIL_ADDRESS`,`PASSWORD_HASH`,`REAL_NAME`,`DISPLAY_NAME`,`GENDER`,`PICTURE_LOCATION`,`COUNTRY`,`FIRST_NAME`,`LAST_NAME`,`DATE_OF_BIRTH`,`VERIFICATION_IDENTIFIER`,`REFERRAL_ID`,`EXTERNAL_ID`,`PROVIDER_NAME`,`USER_REGISTRATION_ERROR`,`BLOCKED`,`PLAYER_ID`) VALUES " ,
"(ROBOT_ID, 'robotROBOT_ID@example.com', NULL, '', 'Robot ROBOT_ID', 'robot ROBOT_ID', 'M', 'http://www.breakmycasino.com/avatars/public/avatar1.png', 'GB', NULL, NULL, NULL, NULL, NULL, NULL, 'YAZINO', NULL, 0, ROBOT_ID)"

puts "ALTER TABLE LOBBY_USER AUTO_INCREMENT = #{@robots+1};"

create_queries "INSERT INTO `ACCOUNT` (`ACCOUNT_ID`, `NAME`,`BALANCE`,`VERSION`,`CREDIT_LIMIT`,`OPEN`,`PARENT_ACCOUNT_ID`) VALUES ",
"(ROBOT_ID, 'PLAY_FOR_FUN:robot ROBOT_ID', 10000000000.0000, 0, 0.0000, 1, NULL)"

puts "ALTER TABLE ACCOUNT AUTO_INCREMENT = #{@robots+1};"

create_queries "INSERT INTO `PLAYER` (`PLAYER_ID`,`PARTNER_ID`,`EXTERNAL_ID`,`ACCOUNT_ID`,`USER_ID`,`VERSION`,`NAME`,`RELATIONSHIPS`,`PICTURE_LOCATION`,`PREFERRED_CURRENCY`,`ACHIEVEMENTS`,`REWARDS`,`ACHIEVEMENT_PROGRESS`,`LEVEL`,`TSCREATED`,`IS_INSIDER`,`LAST_TOPUP_DATE`,`PREFERRED_PAYMENT_METHOD`) VALUES ",
"(ROBOT_ID, 'PLAY_FOR_FUN', 'ROBOT_ID', ROBOT_ID, ROBOT_ID, 0, 'robot ROBOT_ID', NULL, 'http://www.breakmycasino.com/avatars/public/avatar1.png', 'USD', '', NULL, NULL, NULL, now(), NULL, now(), NULL)"

puts "INSERT INTO `$SEQUENCE` (`TSALLOCATED`,`ID`) VALUES (now(), #{@robots+1});"

puts "insert into YAZINO_LOGIN (email_address, password_hash, password_type, salt, user_id)
select email_address, '4gn+bHlQsXNR118FJyH3grYgfYg=', 'PBKDF2', UNHEX('D4080D7905338AAF'), user_id
from LOBBY_USER
where user_id not in (select user_id from YAZINO_LOGIN);"

