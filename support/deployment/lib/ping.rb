module Yazino
  class Ping

    def self.ping(host, wait_seconds = 10)
      if RUBY_PLATFORM.include?('darwin')
        wait_time = wait_seconds * 1000
      else
        wait_time = wait_seconds
      end
      %x(ping -W #{wait_time} -c 1 #{host} 1>/dev/null 2>/dev/null)
      $?.exitstatus == 0
    end

  end
end
