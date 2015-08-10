require 'method/deployment_method'
require 'ssh'
require 'socket'

class DatabaseMethod < DeploymentMethod

  EXTRACT_DIR = "/tmp/db.deploy"

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @db_user = 'root'
    @db_password = nil
    @db_name = 'database'
    @db_host = 'localhost'
    @delta_location = 'deltas'
    @db_type = 'mysql'

    @config = config
    @artefact = artefact
    @db_user = params['db_user'] if params.has_key?('db_user')
    @db_password = params['db_password'] if params.has_key?('db_password')
    @db_name = params['db_name'] if params.has_key?('db_name')
    @delta_location = params['delta_location'] if params.has_key?('delta_location')
    @db_type = params['db_type'] if params.has_key?('db_type')
  end

  def maintenance_required?(options = {})
    true
  end

  def deploy(hosts, options = {})
    print "DEBUG: DatabaseMethod: Executing deploy(#{hosts}) of #{@artefact} on database #{@db_name}\n".foreground(:green) if @config.debug?

    hosts.each do |host|
      db_type = @config.params_for_host(host)['db_type'] || @db_type
      db_user = @config.params_for_host(host)['db_user'] || @db_user
      db_name = @config.params_for_host(host)['db_name'] || @db_name

      delta_location = @config.params_for_host(host)['delta_location'] || @delta_location
      database = database_for(db_type, db_user, @db_password, db_name, host)

      print "DEBUG: DatabaseMethod: host is #{host}, DB Type is #{db_type}\n".foreground(:green) if @config.debug?
      Yazino::SSH.start(host, @config.ssh_options) do |ssh|
        ssh.exec("if [ -d #{EXTRACT_DIR} ]; then sudo rm -rf #{EXTRACT_DIR}; fi && sudo mkdir #{EXTRACT_DIR} && sudo chown spanner:spanner #{EXTRACT_DIR} && cd #{EXTRACT_DIR} && sudo unzip -j #{staged_path} '#{delta_location}/*'")

        incomplete_deltas = database.incomplete_delta_numbers(ssh)
        raise "Incomplete delta application on #{host}: deltas #{incomplete_deltas} not applied correctly" if incomplete_deltas.size > 0

        last_change = database.last_change_applied(ssh)
        latest_change = latest_change_available(ssh)

        print "DEBUG: DatabaseMethod: last change is #{last_change}; latest change is #{latest_change}\n".foreground(:green) if @config.debug?

        if latest_change > last_change
          print "* Applying changes #{last_change + 1}..#{latest_change} for #{@artefact} on #{host}.#{@db_name}\n"

          database.run_deltas(ssh, EXTRACT_DIR, last_change, latest_change)

          database.reset_sequence_privileges(ssh)
        else
          print "* No database changes to apply for #{@artefact} on #{host}.#{@db_name}\n"
        end
      end
    end
  end

  private

  def latest_change_available(ssh)
    if ssh.exec("cd #{EXTRACT_DIR} && sudo ls *.sql | grep -e '[0-9]\\\{1,\\\}\.sql' | sort -n | tail -n 1") =~ /(\d+)\.sql/
      $1.to_i
    else
      raise "Cannot find any changesets"
    end
  end

  def database_for(db_type, db_user, db_password, db_name, host)
    if db_type == 'mysql'
      MySQL.new(db_user, db_password, db_name)
    elsif db_type == 'postgres'
      Postgres.new(db_user, db_password, db_name, host)
    else
      raise "Unknown db type: #{db_type}"
    end
  end

  def eql?(object)
    return false if !super.eql?(object)

    return object.db_name == @db_name\
        && object.artefact == @artefact\
        && object.db_user == @db_user\
        && object.db_password == @db_password\
        && object.db_host == @db_host\
        && object.delta_location == @delta_location
  end

  def hash
    [super.hash, @db_name, @artefact, @db_user, @db_password, @db_host, @delta_location].hash
  end

end

class Database

  def last_change_applied(ssh)
    if ssh.exec(cmd("SELECT max(change_number) last_change FROM changelog")) =~ /last_change:?\s*\|?\s*(\d+)/
      $1.to_i
    else
      0
    end
  end

  def incomplete_delta_numbers(ssh)
    db_change_numbers = ssh.exec(cmd("SELECT change_number FROM changelog WHERE complete_dt IS NULL"))
    if db_change_numbers !~ /ERROR/
      db_change_numbers.scan(/change_number:?\s*\|?\s*(\d+)$/).flatten.map {|number| number.to_i}
    else
      raise "Failed to read changelog"
    end
  end

  private

end

class Postgres < Database

  def initialize(db_user, db_password, db_name, host)
    @db_user = db_user
    @db_password = db_password
    @db_name = db_name
    @host = host
  end

  def run_deltas(ssh, delta_dir, last_change, latest_change)
    from = "#{ENV['USER']}@#{Socket::gethostname}"
    # Deltas must be run individually for dbdeploy 3
    ((last_change + 1)..latest_change).each do |change_number|
      exec(ssh, file("#{delta_dir}/#{change_number}.sql"))
      change_log_file = "#{delta_dir}/update-changelog-#{change_number}.sql"

      update_sql = "INSERT INTO changelog (change_number,complete_dt,applied_by,description) VALUES (#{change_number},NOW(),\'#{from}\',\'#{change_number}.sql\');"
      ssh.exec("echo \\\"#{update_sql}\\\" > #{change_log_file} && #{file(change_log_file)} 2>&1")
    end
  end

  def reset_sequence_privileges(ssh)
    print "* Resetting the sequence privileges on sequences\n"
    ssh.exec("sudo -u postgres psql -d #{@db_name} -qAt -c \\\"SELECT 'GRANT update ON ' || relname || ' TO #{@db_name};' FROM pg_statio_all_sequences WHERE schemaname = 'public'\\\" | sudo -u postgres psql -d #{@db_name}")
  end

  private

  def cmd(sql)
    psql("-x -c '#{sql}' ")
  end

  def file(filename)
    psql("< #{filename}")
  end

  def exec(ssh, command)
    ssh_out = ssh.exec("#{command} 2>&1")
    throw "ERROR: psql returned #{ssh_out}" if ssh_out =~ /ERROR/
  end

  def psql(arguments)
    "psql -U #{@db_user} -d #{@db_name} #{arguments}"
  end

end

class MySQL < Database

  def initialize(db_user, db_password, db_name)
    @db_user = db_user
    @db_password = db_password
    @db_name = db_name
  end

  def run_deltas(ssh, delta_dir, last_change, latest_change)
    begin
      update_sql = build_update_script(delta_dir, last_change + 1, ssh)
      ssh.exec(file(update_sql))

    rescue Exception => e
      if e.message =~ /(ERROR \d+.*)/
        raise "Failed with database error: #{$1}"
      else
        raise e
      end
    end
  end

  def reset_sequence_privileges(ssh)
      #  don't need to do for MySql
  end

  private

  def cmd(sql)
    mysql("-e '#{sql}\\G'")
  end

  def file(filename)
    mysql("< #{filename}")
  end

  def build_update_script(working_dir, from_change, ssh)
    update_sql = "update.sql"
    from = "#{ENV['USER']}@#{Socket::gethostname}"
    create_changelog = "INSERT INTO changelog \\(change_number,delta_set,start_dt,applied_by,description\\) VALUES \\(FILE,\\\'main\\\',NOW\\(\\),\\\'#{from}\\\',\\\'FILE.sql\\\'\\)\\#"
    update_changelog = "UPDATE changelog SET complete_dt=NOW\\(\\) WHERE change_number=FILE\\#"

    # Sorry about the following, but it's more efficient than outputting + uploading a script
    command = "cd #{working_dir} && echo -e 'DELIMITER #\\n' > #{update_sql} && ls *.sql | grep -e '[0-9]\\{1,\\}\\.sql' | awk '{gsub(/\\.sql/,\\\"\\\")};1' | awk '{if (\\$1 >= #{from_change}) print $1}' | sort -n | xargs -I FILE bash -c \\\"echo -- Change FILE.sql >> #{update_sql}; echo #{create_changelog}  >> #{update_sql}; cat FILE.sql | sed '/--\\/\\/@UNDO/,$ d' >> #{update_sql}; echo >> #{update_sql}; echo #{update_changelog} >> #{update_sql}; echo >> #{update_sql}\\\""
    ssh.exec(command)
    "#{working_dir}/#{update_sql}"
  end

  def mysql(arguments)
    mysql_cmd = "mysql -u #{@db_user}"
    mysql_cmd += " -p#{@db_password}" if !@db_password.nil?
    mysql_cmd += " -h #{@db_host}" if !@db_host == 'localhost'
    "#{mysql_cmd} #{@db_name} #{arguments}"
  end

end

