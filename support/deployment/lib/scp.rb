module Yazino
  class SCP

    def self.start(hostname, params = {}, &block)
      ssh = Yazino::SCP.new(hostname, params)
      if block_given?
        yield ssh
      else
        ssh
      end
    end

    def initialize(hostname, params = {})
      @username = ENV['USER']
      @port = 22
      @exit_on_error = true

      @hostname = hostname
      @username = params['username'] if params.has_key?('username')
      @port = params['port'] if params.has_key?('port')
      @key = params['key'] if params.has_key?('key')
      @args = params['args'] if params.has_key?('args')
      @exit_on_error = params['exit_on_error'] if params.has_key?('exit_on_error')

      if @key
        if @key !~ /^\//
          @key = "#{File.dirname(__FILE__)}/../config/#{@key}"
        end

        raise "Cannot find SSH key #{@key}" if !File.exists?(@key)

        %x(chmod 600 #{@key})
      end
    end

    def upload_to(destination, filenames)
      dest_path = "#{@hostname}:#{destination}"
      dest_path = "#{@username}@#{dest_path}" if !@username.nil?
      exec_scp("#{filenames.join(' ')} #{dest_path}")
    end

    private

    def exec_scp(command)
      raise "Cannot execute empty command" if command.nil? || command.length == 0

      scp_command = "scp -q -o UserKnownHostsFile=/dev/null -o BatchMode=yes -o StrictHostKeyChecking=no -o LogLevel=ERROR -c arcfour "
      scp_command = "#{scp_command}-i #{@key} " if !@key.nil?
      scp_command = "#{scp_command}-P #{@port} " if @port
      ssh_command = "#{ssh_command}#{@args} " if @args
      scp_command = "#{scp_command} #{command}"

      output = %x(#{scp_command} 2>&1)
      raise "Failed to copy: #{scp_command}: output was #{output}" if $?.exitstatus != 0 && @exit_on_error
      output
    end

  end
end
