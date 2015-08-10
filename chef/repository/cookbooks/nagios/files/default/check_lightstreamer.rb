#!/usr/bin/env ruby

OPTIONS = {'-c' => :critical, '-w' => :warning, '-m' => :metric}
STATES = { :ok => 0, :warning => 1, :critical => 2, :unknown => 3, :dependent => 4}
COMPARATORS = {:greater => Proc.new {|a, b| a >= b}, :lesser => Proc.new {|a, b| a <= b}}
DEFAULT_CRITICAL = 10
DEFAULT_WARNING = 100
LOG_FILE = '/var/log/lightstreamer/Yazino.log'
USAGE_MESSAGE = """Usage: #{__FILE__} -m <metric> [-c critical_threshold=#{DEFAULT_CRITICAL}] [-w warning_threshold=#{DEFAULT_WARNING}] [--less-than]
  Where metric is one of:
    active_threads
    max_threads
    pending_tasks
    subscriptions
    delayed_subscriptions
    pending_unsubscriptions
"""

def exit_with(code, message)
  puts "#{code.to_s.upcase} - #{message}"
  exit STATES[code]
end

def parse_args(raw_args)
  raise USAGE_MESSAGE if raw_args.length < 2

  args = {:critical => DEFAULT_CRITICAL, :warning => DEFAULT_WARNING, :order => :greater}

  index = 0
  while index < raw_args.length
    case raw_args[index]
    when '--less-than'
      args[:order] = :lesser
      index += 1
    else
      raise "Unknown argument: '#{raw_args[index]}'; #{USAGE_MESSAGE}" unless OPTIONS.has_key?(raw_args[index])
      args[OPTIONS[raw_args[index]]] = raw_args[index + 1]
      index += 2
    end
  end

  raise USAGE_MESSAGE if args[:metric].nil?

  args
end

def get_metrics
  active_threads, max_threads, pending_tasks = %x[grep 'Thread pool status:' #{LOG_FILE} | tail -1].match(/active (\d+); max (\d+); largest \d+; pending (\d+)/).captures
  subscriptions, pending_unsubscriptions, delayed_subscriptions = %x[grep 'Health status:' #{LOG_FILE} | tail -1].match(/subscriptions (\d+); pending unsubscriptions (\d+); delayed subscriptions (\d+)/).captures

  {
    :active_threads => active_threads.to_i,
    :max_threads => max_threads.to_i,
    :pending_tasks => pending_tasks.to_i,
    :subscriptions => subscriptions.to_i,
    :pending_unsubscriptions => pending_unsubscriptions.to_i,
    :delayed_subscriptions => delayed_subscriptions.to_i
  }
end

begin
  args = parse_args(ARGV)

  exit_with(:unknown, "Couldn't find log file: #{LOG_FILE}") if !File.exists?(LOG_FILE)

  metrics = get_metrics

  raise "Key doesn't exist: #{args[:metric]}" unless metrics.has_key? args[:metric].to_sym
  metric_value = metrics[args[:metric].to_sym]

  result = if COMPARATORS[args[:order]].call(metric_value, args[:critical].to_i)
    :critical
  elsif COMPARATORS[args[:order]].call(metric_value, args[:warning].to_i)
    :warning
  end || :ok

  exit_with(result, "#{args[:metric]} is #{metric_value} | #{args[:metric]}=#{metric_value};#{args[:warning]};#{args[:critical]};0; #{metrics.delete_if {|key,value| key == args[:metric].to_sym}.to_a.collect{|pair| pair.join('=')}.join(' ')}")

rescue => e
  exit_with :unknown, e
end
