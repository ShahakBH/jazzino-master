require 'rubygems'
require 'bundler/setup'

require 'rainbow'

module Yazino
  class SSH

    def self.start(hostname, params = {}, &block)
      ssh = Yazino::SSH.new(hostname, params)
      if block_given?
        yield ssh
      else
        ssh
      end
    end

    def initialize(hostname, params = {})
      @debug = false
      @debug = params['debug'] if params.has_key?('debug')
      print "DEBUG: SSH: Creating with host #{hostname} and params #{params}\n".foreground(:green) if @debug

      @username = ENV['USER']
      @port = 22
      @exit_on_error = true
      @print_commands = false

      @hostname = hostname
      @username = params['username'] if params.has_key?('username')
      @port = params['port'] if params.has_key?('port')
      @key = params['key'] if params.has_key?('key')
      @args = params['args'] if params.has_key?('args')
      @exit_on_error = params['exit_on_error'] if params.has_key?('exit_on_error')
      @quiet = '-q' unless params['verbose']

      if @key
        if @key !~ /^\//
          @key = "#{File.dirname(__FILE__)}/../config/#{@key}"
        end

        raise "Cannot find SSH key #{@key}" if !File.exists?(@key)

        %x(chmod 600 #{@key})
      end
    end

    def exec(command, log_file = nil)
      exec_ssh(command, log_file)
    end

    private

    def exec_ssh(command, log_file = nil)
      raise "Cannot execute empty command" if command.nil? || command.length == 0

      ssh_command = "ssh #{@quiet} -o UserKnownHostsFile=/dev/null -o BatchMode=yes -o StrictHostKeyChecking=no -o LogLevel=ERROR -o ConnectTimeout=5 -o ConnectionAttempts=3 -c arcfour -m hmac-md5-96 "
      ssh_command = "#{ssh_command}-i #{@key} " if @key
      ssh_command = "#{ssh_command}-p #{@port} " if @port
      ssh_command = "#{ssh_command}#{@args} " if @args
      ssh_command = "#{ssh_command}#{@username}@" if @username
      ssh_command = "#{ssh_command}#{@hostname} \"#{command}\""

      print "DEBUG: SSH exec: #{ssh_command}\n".foreground(:green) if @debug

      output = ''
      IO.popen("#{ssh_command} 2>&1") do |data|
        while line = data.gets
          print line unless @quiet
          output += line
        end
      end
      ssh_exit_status = $?.exitstatus

      if log_file
        begin
          File.open(log_file, 'w') { |file| file << output }
        rescue Exception => e
          print "! ERROR: Couldn't write log to #{log_file}: #{e}\n".foreground(:red)
        end
      end

      raise "Failed to execute: #{ssh_command}: exit code #{ssh_exit_status}; output was: #{output}" if ssh_exit_status != 0 && @exit_on_error
      output
    end

  end
end
