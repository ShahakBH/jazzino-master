def update_limit(file, type, name, value)
  execute "update #{type} #{name} in #{file}" do
    command "sed -i'' -e 's%^\\(\\*[\\t ]\\+#{type}[\\t ]\\+#{name}[\\t ]\\+\\)[0-9]\\+%\\1#{value}%g' #{file}"
    action :run
    only_if "grep -E '^\\*.*#{type}.*#{name}' #{file}"
  end

  execute "create #{type} #{name} in #{file}" do
    command "echo -e '*\t#{type}\t#{name}\t#{value}' >> #{file}"
    action :run
    not_if "grep -E '^\\*.*#{type}.*#{name}' #{file}"
  end
end

def update_profile_ulimit(type, value)
  execute "update ulimit -#{type} in /etc/profile" do
    command "sed -i'' -e 's%^\\(ulimit[ \\t]\\+-#{type}[ \\t]\\+\\)[0-9]\\+%\\1#{value}%g' /etc/profile"
    action :run
    only_if "grep -E '^ulimit -#{type}' /etc/profile"
  end

  execute "create ulimit -#{type} in /etc/profile" do
    command "echo -e 'ulimit -#{type} #{value}' >> /etc/profile"
    action :run
    not_if "grep -E '^ulimit -#{type}' /etc/profile"
  end
end

LIMITS_CONF = '/etc/security/limits.conf'

update_limit(LIMITS_CONF, 'soft', 'nofile', node[:limits][:files][:soft])
update_limit(LIMITS_CONF, 'hard', 'nofile', node[:limits][:files][:hard])
update_profile_ulimit('n', node[:limits][:files][:user])

update_limit(LIMITS_CONF, 'soft', 'nproc', node[:limits][:processes][:soft])
update_limit('/etc/security/limits.d/90-nproc.conf', 'soft', 'nproc', node[:limits][:processes][:soft])
update_limit(LIMITS_CONF, 'hard', 'nproc', node[:limits][:processes][:hard])
update_profile_ulimit('u', node[:limits][:processes][:user])
