module Splots

  def self.normal_play(context)
    newContext = context
    if context[:allowed_actions].size == 0
      puts "no allowed actions do nothing"
    end
    if context[:allowed_actions].include?("Reset")
      begin
        puts "Reset Allowed Action received ... resetting"
        send_command "Reset"
      rescue
        puts "failed to reset"
      end
    end
    if context[:can_bet] && context[:allowed_actions].include?("Bet") && (now - context[:last_bet_ts] > 1)
      newContext.update({
                            :last_bet_ts => now,
                            :can_bet => false,
                            :has_gambled => false,
                            :has_collected => false,
                            :command_result => false
                        })
      begin
        betOption = context[:bet_options].sample
        lineOption = context[:line_options].sample

        #send_command "Lines", [lineOption] # Alter lines

        #send_command "BetPerLine", [betOption] # Alter Bet

        puts "sending bet command"
        send_command "Bet", [betOption, lineOption] # bet placement, line
      rescue
        puts "failed to split options"
      end

      newContext
    end

    if context[:has_gambled] && !context[:has_collected] && context[:allowed_actions].include?("Collect")
      newContext.update({
                            :last_collect_ts => now,
                            :has_gambled => true,
                            :has_collected => true,
                            :command_result => false
                        })
      puts "sending bet Collect"
      send_command "Collect"
      return newContext
    end

    if !context[:has_collected] && !context[:has_gambled] && context[:allowed_actions].include?("Guess")
      newContext.update({
                            :last_collect_ts => now,
                            :has_gambled => true,
                            :command_result => false
                        })
      redOrBlack = ["1", "2"]
      puts "sending bet Guess"
      send_command "Guess", [redOrBlack.sample]
      return newContext
    end

    context
  end


  def self.random_play(context)
    commandArray = ["Bet", "Collect", "Guess", "randomString", "Join", "Reset", "Lines", "BetPerLine"]
    betlines = ["1", "3", "5", "7", "9", "10", "12", "15", "20", "25", "1234567"]
    betAmount = ["1", "2", "3", "4", "5", "10", "20", "50", "100", "200", "300", "400", "500", "700", "1000", "2000"]
    guess = ["0", "1", "2", "3"]

    command = commandArray.sample

    case command
      when "Bet"
        send_command command, [betlines.sample, betAmount.sample]
      when "Guess"
        send_command command [guess.sample]
      else
        send_command command
    end

    context
  end

  def self.bet_per_line(context)
    if context[:allowed_actions].include?("BetPerLine")
        betOption = context[:bet_options].sample
        send_command("BetPerLine", [betOption])
    end
    context
  end

  def self.parse_game_status(message)
    lineOptions = $user_context[:line_options];
    betOptions = $user_context[:bet_options];;

    puts_with_timestamp message
    changes = message["changes"]
    puts "Changes = #{changes}"
    lines = changes.split("\n")
    puts "Lines = #{lines}"
    new_changes = []
    lines[(5..lines.length)].each do |line|
      details = line.split("\t")
      puts "Details = #{details}"

      if details[1] == "LineOptions"
        lineOptions = details[3].split(" ")
      end

      if details[1] == "BetOptions"
        betOptions = details[3].split(" ")
      end
      new_changes << details[1] if details[0].to_i > $user_context[:increment]
    end
    new_context = {
        :game_id => message["gameId"],
        :playing => message["isAPlayer"] == "true",
        :increment => lines[2].to_i,
        :allowed_actions => lines[3].split("\t"),
        :line_options => lineOptions,
        :bet_options => betOptions
    }

    if !$user_context[:command_result] || new_changes.include?("BetPlaced")
      latency = now - $user_context[:last_bet_ts]
      puts_with_timestamp "Latency = #{latency}"
      $stats[:last_command_latency] = (latency * 1000).round(3)
      new_context[:command_result] = true,
      new_context[:can_bet] = true
    end

    new_context
  end

end