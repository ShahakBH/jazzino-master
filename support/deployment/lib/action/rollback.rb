require 'ssh'

class RollbackAction < DeployAction

  alias :deploy_exec :exec

  def initialize(config)
    @config = config
    
    super(config)
  end
  
  def exec(options, components)    
    @config.hosts_for_roles(['staging']).each do |staging_host|
      Yazino::SSH.start(staging_host, @config.ssh_options) do |ssh|
        latest_files = ssh.exec("ls -l #{@config.base_staging_dir} --sort=time | grep -e '^d' | head -n 2 | awk '{print \\$9}'").split("\n")
        raise "Cannot find previous release to rollback to" if latest_files.size < 2
      
        latest_release = latest_files[0]
        previous_release = latest_files[1]
    
        latest_link = @config.latest_staging_dir
        if ssh.exec("if [ \\$(readlink #{latest_link}) == '#{@config.base_staging_dir}/#{previous_release}' ]; then echo ROLLEDBACK; fi") =~ /ROLLEDBACK/
          raise "Release is already pointing at the previous release (#{previous_release}); latest release was #{latest_release}"
        end
    
        print "* Rolling back to #{previous_release} from #{latest_release}\n"
    
        @config.set_staged_name(previous_release)
        options[:deploy] = previous_release
    
        ssh.exec("if [ -h '#{latest_link}' ]; then sudo rm -f #{latest_link}; fi && sudo ln -s #{@config.staging_dir} #{latest_link}")
      end
    end
    
    deploy_exec(options, components)
  end
  
end
