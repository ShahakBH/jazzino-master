class DeploymentChecker

  attr_reader :mocha, :steps, :ssh_opts, :ssh, :scp

  def initialize(mocha)
    @mocha = mocha
    @steps = @mocha.sequence('steps')
    @steps_for_host = {}
    @ssh_mocks = {}
    @maintenance = WebMaintenance.any_instance
    define_default_behaviours
  end

  def steps_for(host)
    if @steps_for_host[host].nil?
      @steps_for_host[host] = @mocha.sequence('steps_' + host)
    end
    @steps_for_host[host]
  end

  def staged_to(hosts)
    execute_on hosts do |ssh|
    end
  end

  def maintenance_on
    @maintenance.expects(:on).in_sequence(@steps)
  end

  def maintenance_off
    @maintenance.expects(:off).in_sequence(@steps)
  end

  def jetty_stopped_on(hosts)
    execute_on hosts do |ssh|
      ssh.exec("if [ -f /var/staging/latest/web-lobby.war -a -f /opt/jetty/webapps/root.war ]; then cmp /var/staging/latest/web-lobby.war /opt/jetty/webapps/root.war &> /dev/null; echo \\$?; else echo 1; fi", "1")
      ssh.exec("sudo su -c '/sbin/service jetty status 2>&1'", "jetty running")
      ssh.exec("sudo su -c '/sbin/service jetty stop'")
    end
  end

  def deployed_web_to(hosts)
    execute_on hosts do |ssh|
      ssh.exec("if [ -f /var/staging/latest/web-lobby.war -a -f /opt/jetty/webapps/root.war ]; then cmp /var/staging/latest/web-lobby.war /opt/jetty/webapps/root.war &> /dev/null; echo \\$?; else echo 1; fi", "1")
      ssh.exec "sudo rm -rf /opt/jetty/webapps/root.war && sleep 2 && sudo cp /var/staging/latest/web-lobby.war /opt/jetty/webapps/root.war"
    end
  end

  def execute_on(hosts)
    hosts.each do |host|
      ssh = SSHContext.new(self, host)
      yield ssh
    end
  end

  def ssh_mock_for(host)
    if @ssh_mocks[host].nil?
      mock = @mocha.mock()
      mock.stubs(:exec).returns("1")
      @ssh_mocks[host] = mock
    end
    @ssh_mocks[host]
  end

  def removed_from_load_balancer(host)
    WebLoadBalancer.any_instance.expects(:remove).with(host)
  end

  def added_to_load_balancer(host)
    WebLoadBalancer.any_instance.expects(:wait_until_added).with(host)
  end

  private

  def define_default_behaviours
    Yazino::Config.any_instance.stubs(:timestamp).returns("<timestamp>")
    Yazino::SCP.any_instance.stubs(:upload_to).returns("1")
    Yazino::SSH.stubs(:start).yields(ssh_mock_for("<default>")).returns(true)
    Yazino::Ping.stubs(:ping).returns(true)
    Yazino::Ping.stubs(:ping_redshift).returns(true)
    WebLoadBalancer.any_instance.stubs(:remove)
    WebLoadBalancer.any_instance.stubs(:wait_until_added)
  end

end

class SSHContext
  def initialize(context, host)
    @context = context
    @host = host
    @mock = @context.ssh_mock_for(host)
    #needs to return true otherwise DeploymentMethod.remote_files_differ always returns nil
    Yazino::SSH.expects(:start).at_least_once.with(host, @context.mocha.anything).yields(@mock).returns(true).in_sequence(@context.steps_for(@host))
  end

  def exec(command, return_value = "")
    @mock.expects(:exec).with(command).returns(return_value).in_sequence(@context.steps_for(@host))
  end
end
