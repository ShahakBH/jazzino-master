package "nss-pam-ldapd" do
  action :install
end

package "openldap-clients" do
  action :install
end

package "mod_authz_ldap" do
  action :install
end

execute "configure_ldap_auth" do
    command "authconfig --enableldap --enableldapauth --enablemkhomedir --ldapserver=fw.london.yazino.com --ldapbasedn=dc=deus,dc=signtechno,dc=com --update"
    action :run
end

cookbook_file "/etc/openldap/ldap.conf" do
  source "etc_openldap_ldap.conf"
  owner "root"
  group "root"
  mode 0644
end
