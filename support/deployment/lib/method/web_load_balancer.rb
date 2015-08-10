require 'ssh'

class WebLoadBalancer

  def initialize(config)
    @sleep_time = 10
    @loadbalancer_sleep_time = 60
    @tries = 18
    @config = config
    @check = WebLoadBalancerCheck.new(config)
    @load_balancer_suspend_file = "/etc/senet/suspendedFromLoadBalancer"
  end


  def remove(host)
    removed = remove_from_load_balancer host
    if removed
      wait_until(host, false, "unavailable")
      extra_load_balancer_sleep
    end
  end

  def wait_until_added(host)
    added = add_to_load_balancer host
    if added
      wait_until(host, true, "available")
      extra_load_balancer_sleep
    end
  end

  def wait_until(host, target, message)
    try = 1
    while try < @tries
      print "* Waiting for #{host} to become #{message} [#{try}/#{@tries}]\n"
      if (is_available(host)) == target
        print "* Host #{host} is now #{message}\n"
        return
      else
        try += 1
        sleep @sleep_time
      end
    end
    raise "Exceeded #{@tries} tries and #{host} is not #{message}!"
  end

  private

  def remove_from_load_balancer(host)
    removed = true
    Yazino::SSH.start(host, @config.ssh_options) do |ssh|
      print "DEBUG: Creating #{@load_balancer_suspend_file}\n".foreground(:green) if @config.debug?
      removed = ssh.exec("if [ ! -f #{@load_balancer_suspend_file} ]; then sudo touch #{@load_balancer_suspend_file}; else echo 'FILE_EXISTS'; fi") !~ /FILE_EXISTS/
    end
    removed
  end

  def add_to_load_balancer(host)
    added = true
    Yazino::SSH.start(host, @config.ssh_options) do |ssh|
      print "DEBUG: Removing #{@load_balancer_suspend_file}\n".foreground(:green) if @config.debug?
      added = ssh.exec("if [ -f #{@load_balancer_suspend_file} ]; then sudo rm -f #{@load_balancer_suspend_file}; else echo 'REMOVED'; fi") !~ /REMOVED/
    end
    added
  end

  def extra_load_balancer_sleep
    print "* Allowing extra #{@loadbalancer_sleep_time} seconds for load balancer to update\n"
    sleep @loadbalancer_sleep_time
  end

  def is_available(host)
    print "DEBUG: Checking if #{host} is available\n".foreground(:green) if @config.debug?
    @check.is_available(host)
  end
end

class WebLoadBalancerCheck
  
  def initialize(config)
    @config = config
    @max_time = 2
    @path = "/command/loadBalancerStatus"
  end

  def is_available(host)
    content = run_curl(host)
    http_code = content[-3..-1].to_i
    http_code == 200 && !content.match(/.*\"status\": \"okay\".*/).nil?
  end

  def run_curl(host)
    output = %x(curl -m #{@max_time} -sL -w "%{http_code}" #{host}#{@path} 2>&1)
    print "DEBUG: Curl output:\n#{output}\n".foreground(:green) if @config.debug?
    output
  end
end
