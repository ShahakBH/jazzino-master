#!/usr/bin/ruby
#file = File.new("/tmp/output.txt", "r")

if ARGV.length != 2
  puts "Usage: <lookupgroup> <lookuplocators>"
  exit 3
end

file = %x(LOOKUPGROUPS='#{ARGV[0]}' LOOKUPLOCATORS='#{ARGV[1]}' /opt/gigaspaces/bin/gs.sh space list 2>&1)

@threshold = {}
@threshold['Account']= {:w => 1000000, :c => 5000000}
@threshold['AccountPersistenceRequest']= {:w => 10000, :c => 50000}
@threshold['AccountTransactionPersistenceRequest']= {:w => 10000, :c => 50000}
@threshold['ExternalTransactionPersistenceRequest']= {:w => 1000, :c => 5000}
@threshold['Player']= {:w => 1000000, :c => 5000000}
@threshold['PlayerSession']= {:w => 10000, :c => 50000}
@threshold['Table']= {:w => 100000, :c => 500000}
@threshold['TableRequestWrapper']= {:w => 1000, :c => 5000}
@threshold['HostDocumentWrapper']= {:w => 100, :c => 500}
@threshold['TableTransactionRequest']= {:w => 100, :c => 500}

@performance_data = {}
@messages = {}
@statuses = {:undefined => -1, :ok => 0, :warning => 1, :critical => 2, :unknown => 3}
@exit_code = @statuses[:undefined]

def add_exit_code(status)
  code = @statuses[status] || @statuses[:undefined]
  if code > @exit_code
    @exit_code = code
  end
end

def add_performance_data(name, count)
  @performance_data[name] = 0 unless @performance_data.has_key? name
  @performance_data[name] = @performance_data[name] + count
end

def add_message(name, count, threshold)
  if count > threshold[:c]
    add_exit_code :critical
    @messages[name] = "CRITICAL - #{count} > #{threshold[:c]}"
  elsif count > threshold[:w]
    add_exit_code :warning
    @messages[name] = "WARNING - #{count} > #{threshold[:w]}"
  else
    #@messages[name] = "OK - #{count}"
    add_exit_code :ok
  end

end

file.each_line do |line|
  tokens = line.match /Class Name:\s*(\S+)\s*Objects count:\s*(\d+)/
  if !tokens.nil? and tokens.length == 3
    name = tokens[1].split(".").last
    count = tokens[2].to_i
    add_performance_data(name, count)
  end
end

@names = []

@performance_data.map do |name, count|
  if @threshold.has_key? name
    @names << name
    add_message(name, count, @threshold[name])
  end
end

if @exit_code == @statuses[:undefined]
  puts "No objects found"
  exit @statuses[:unknown]
end

@messages = Hash[@messages.sort]

display_message = @messages.map { |name, message| "#{name}=#{message}" }.join(", ")
formatted_performance = @names.sort.map { |name| "'#{name}'=#{@performance_data[name]}" }.join(" ")

final_result = "#{display_message} | #{formatted_performance}"

if final_result.length >= 1024
  puts "Response too long"
  exit @statuses[:critical]
end

puts final_result
exit @exit_code
