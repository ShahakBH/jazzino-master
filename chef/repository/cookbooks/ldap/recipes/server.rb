include_recipe "ldap::default"

package "openldap" do
  action :install
end

package "openldap-servers" do
  action :install
end

template "/bin/createLDAPhomes.sh" do
  source "createLDAPhomes.sh.erb"
  owner "root"
  group "root"
  mode 0700
  variables(
    :allowed_group => node[:ldap][:createLDAPhomes][:allowed_group]
  )
end

execute "create_users_to_home_link" do
  command "if [ ! -L '/Users' ]; then ln -s /home /Users; fi"
  action :run
end

execute "add_script_to_cron" do
  command "sed --in-place -e 's/^.*createLDAPhomes.sh.*$//g' /var/spool/cron/root; echo '*/1 * * * *  /bin/createLDAPhomes.sh >/dev/null 2>/dev/null' >>/var/spool/cron/root"
  action :run
end

execute "run_create_ldap_homes" do
  command "/bin/createLDAPhomes.sh"
  action :run
end
