require 'rubygems'
require 'bundler/setup'

require 'rainbow'

require 'ssh'
require 'set'

class DumpLogsAction

  def initialize(config, target)
    @config = config
    @target = target
  end

  def requires_artefacts?
    true
  end

  def exec(options, components)
    level = get_log_level(@target)

    logs_to_print = {}

    components_with_logs = components.values.find_all {|component| !component['log'].nil?}
    components_with_logs.each do |component|
      component['deploy_to'].each do |role|
        @config.hosts_for_roles([role]).each do |host|
          logs_for_host = logs_to_print[host] || Set.new
          (component['log']['files'] || []).each {|log_file| logs_for_host << log_file}
          logs_to_print[host] = logs_for_host
        end
      end
    end

    print "\n------------------------------------------------------------------\n".bright
    print " Dumping log files at level #{level.downcase}\n".bright
    print "------------------------------------------------------------------\n".bright

    logs_to_print.each_key do |host|
       Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
          logs_for_host = logs_to_print[host]

          logs_for_host.each do |log_file|
            log_output = ssh.exec(make_grep_statement(level, log_file))
            if log_output.size > 0
              print "\n* #{log_file} on #{host}\n".foreground(:blue)
              print log_output.foreground(:green)
              print "\n\n"
            end
          end
      end
    end
  end

  private

  LOG_LEVELS = {
    'FATAL' => ['FATAL', 'SEVERE'],
    'ERROR' => ['ERROR', 'SEVERE'],
    'WARN' => ['WARN'],
    'INFO' => ['INFO'],
    'DEBUG' => ['DEBUG', 'FINE'],
    'TRACE' => ['TRACE', 'FINER']
    }
  DEFAULT_LOG_LEVEL = 'ERROR'

  def make_grep_statement(log_level, file)
    if file =~ /\*$/
      "sudo grep #{LOG_LEVELS[log_level].reduce("") {|cmd, level| "#{cmd} -e '#{level} '"}} #{file}"
    else
      "if [ -f '#{file}' ]; then sudo grep #{LOG_LEVELS[log_level].reduce("") {|cmd, level| "#{cmd} -e '#{level} '"}} '#{file}'; fi"
    end
  end

  def get_log_level(target)
    clean_target = (target && target.strip.upcase) || DEFAULT_LOG_LEVEL
    if LOG_LEVELS.include?(clean_target)
      clean_target
    else
      DEFAULT_LOG_LEVEL
    end
  end

end
