java_version = "1.8.0_05"
java_package = "oracle-jdk"
java_arch = "x86_64"

host_arch=%x(uname -p).strip
raise "We don't support 32bit platforms." if host_arch == 'i386' || host_arch == 'i686'

execute "kill-java-processes-before-upgrade" do
  command "killall -q java || true"
  only_if {%x(yum list installed | grep oracle-jdk | awk '{print $2}').strip != "#{java_version}-1"}
end

yum_package java_package do
  version "#{java_version}-1"
  arch java_arch
  allow_downgrade true
  action :install
end

link "/opt/java" do
  to "/opt/java-#{java_version}"
  action :create
end

link "/usr/bin/java" do
  to "/opt/java/bin/java"
  action :create
end

link "/usr/bin/javac" do
  to "/opt/java/bin/javac"
  action :create
end

link "/usr/bin/jar" do
  to "/opt/java/bin/jar"
  action :create
end
