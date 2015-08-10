Splots Bot
==========

Installation
------------
errr something to do with rvm ruby bundle install etc etc


Running the Bot
_________________

command line
jruby -I. run_bot.rb -e breakmycasino -b normal_play

I prefer to run it from intellij

edit your run configurations

Ruby Script: ~/Projects/support/performance/splots/run_bot.rb

Script Arguments: -e breakmycasino -b normal_play

Changing the bot environment
----------------------------

Make sure there is a configuration for the environment you are trying to run the bot against in the config/config.yaml

An example config looks like this.

dseeto-centos:
      user: bot_test@yazino.com
      password: yazino
      server: dseeto-centos.london.yazino.com
      gameType: SPLOTS
      platform: ANDROID
      clientId: Slots
      variationName: Slots
      playType: normal_play


user:  is an already existing user on that environment
password: the users password
server: the environment you want the bot to run against
gameType: the one that your environment is under in the YAML, i.e. SPLOTS
platform: the platform that you want the bot to send
clientId: the client id as defined in the game variation table in the DB
variationName: the variation as defined in the game_variation in the DB
playType: the function that is defined in lib/bots/{gameType}.rb that basically has the logic for your bot.






