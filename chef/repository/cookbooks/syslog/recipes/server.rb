include_recipe "syslog::default"

package "openldap-servers" do
  action :install
end

