require 'socket'

directory "/opt/bin" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

directory "/opt/etc" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

directory "/var/log/database-cleanup" do
  owner "postgres"
  group "postgres"
  mode 0755
  action :create
end

AUDIT_COMMAND = "audit_command"
TRANSACTION_LOG = "transaction_log"

@tables = {
    AUDIT_COMMAND =>"audit_ts",
    TRANSACTION_LOG =>"transaction_ts"
}

template "/etc/cron.d/cleanup-database-runner" do
  source "cleanup-runner.erb"
  owner "root"
  group "root"
  mode "0644"
  variables(:table=> AUDIT_COMMAND,
            :table2=>TRANSACTION_LOG
  )
end

@tables.each_pair {|table, timestamp|
  template "/opt/bin/cleanup-#{table}.rb" do
    source "cleanup-table.erb"
    owner "postgres"
    group "postgres"
    mode "0755"
    variables(
        :table=> table,
        :select_query=>"select * from #{table} where #{timestamp} < (now()-interval '3 months')::date",
        :delete_query=>"delete from #{table} where #{timestamp} < (now()  -interval '3 months')::date"
    )
  end

}

