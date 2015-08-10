#!/bin/bash

TARGET=$1
HOSTNAME=$2
SSH_KEY_NAME=$3

if [ -z "$HOSTNAME" ]; then
    HOSTNAME="$TARGET"
fi

SSH_KEY=""
if [ -n "$SSH_KEY_NAME" ]; then
	SSH_KEY="-i $SSH_KEY_NAME"
fi

SHORTNAME=`echo $TARGET | sed 's/[.].*//'`

if [ -z "$TARGET" ]; then
	echo "Usage: $0 <target FQDN> [ip address; defaults to target] [path to ssh-key]"
	exit 1
fi

BOLD=`tput bold`
NORMAL=`tput rmso`

clear

CHEF_SERVER=https://46.101.41.217:8443
DEPLOYMENT_KEY=deployment-key
OUTPUT_FILE=bootstrap-centos.sh

if [ -f "$OUTPUT_FILE" ]; then
	rm -rf $OUTPUT_FILE
	if [ $? -ne 0 ]; then
		echo "Couldn't remove old output file: $OUTPUT_FILE"
		exit
	fi
fi

echo -e "\
#!/bin/bash\n\
\n\
if [ -f \"/etc/yum/pluginconf.d/rhnplugin.conf\" ]; then \n\
    echo \"Derackspacifying Yum...\" \n\
    sed -i '' -e 's%enabled[[:space:]]*=[[:space:]]*1%enabled = 0%g' /etc/yum/pluginconf.d/rhnplugin.conf \n\
    yum-config-manager --enable base extras updates \n\
fi \n\
\n\
echo \"Upgrading the OS to the latest version... This may take a while.\"\n\
\n\
yum -y upgrade\n\
if [ \"\$(hostname)\" != \"$TARGET\" ]; then\n\
    echo \"Setting HOSTNAME to $TARGET\"\n\
    echo -e \"NETWORKING=yes\\\\nHOSTNAME=$TARGET\\\\n\" >/etc/sysconfig/network \n\
    hostname $TARGET \n\
    echo -e \"NETWORKING=yes\\\\nHOSTNAME=$TARGET\\\\n\" >/etc/sysconfig/network \n\
    echo -e \"\\\\n127.0.0.1   $TARGET $SHORTNAME localhost localhost.localdomain localhost4 localhost4.localdomain4\\\\n::1         $TARGET $SHORTNAME localhost localhost.localdomain localhost6 localhost6.localdomain6\\\\n\" >/etc/hosts \n\
fi\n\
# Installing NTP \n\
yum -y install ntp wget\n\
/usr/sbin/ntpdate ntp.apple.com 1>/dev/null 2>/dev/null\n\
\n\
yum groupinstall -y 'Development Tools'\n\
\n\
if [[ \$(yum list installed | grep '^ruby\.' | awk '{print \$2}') =~ ^1.8 ]]; then \n\
	yum remove -y ruby ruby-shadow rubygems \n\
fi \n\
if [[ ! \$(yum list installed | grep '^ruby\.' | awk '{print \$2}') =~ ^2.1.2 ]]; then \n\
	rpm -Uvh http://download.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm\n\
	yum install -y libyaml\n\
	yum install -y ruby-libs\n\
	rpm -Uih http://pkgs.repoforge.org/libyaml/libyaml-0.1.4-1.el6.rf.x86_64.rpm
	rpm -Uvh http://yum.london.yazino.com/centos/6/x86_64/ruby-2.1.2-2.el6.x86_64.rpm
	if [ \$? -ne 0 ]; then \n\
		echo Ruby installation failed \n\
		exit 1 \n\
	fi \n\
fi \n\
\n\
if [ \"\$(service iptables status | grep -i RackConnect)\" ]; then \n\
    echo \"RackConnect detected, no iptables changes will be made\" \n\
elif [[ \$(uname -r) =~ 'el6' ]]; then \n\
	iptables -D INPUT -j REJECT --reject-with icmp-host-prohibited\n\
	iptables -D FORWARD -j REJECT --reject-with icmp-host-prohibited\n\
	/sbin/service iptables save\n\
	/sbin/service iptables start\n\
	chkconfig iptables on\n\
    ip6tables -D INPUT -j REJECT --reject-with icmp6-adm-prohibited\n\
    ip6tables -D FORWARD -j REJECT --reject-with icmp6-adm-prohibited\n\
    /sbin/service ip6tables save\n\
    /sbin/service ip6tables start\n\
    chkconfig ip6tables on\n\
fi\n\
# Installing Chef for dependencies\n\
gem install --no-rdoc --no-ri chef\n\
\n\
# Create knife config \n\
mkdir -p .chef\n\
echo -e \"current_dir = File.dirname(__FILE__) \n\
log_level                :info \n\
log_location             STDOUT \n\
node_name                \x5C\"admin\x5C\" \n\
client_key               \x5C\"#{current_dir}/admin.pem\x5C\" \n\
validation_client_name   \x5C\"chef-validator\x5C\" \n\
validation_key           \x5C\"#{current_dir}/chef-validator.pem\x5C\" \n\
chef_server_url          \x5C\"$CHEF_SERVER\x5C\" \n\
cache_type               \x5C\"BasicFile\x5C\" \n\
cache_options( :path => \x5C\"#{ENV['HOME']}/.chef/checksums\x5C\" ) \n\
cookbook_path            [\x5C\"#{current_dir}/../cookbooks\x5C\"]\" > .chef/knife.rb \n\
echo -e \"-----BEGIN RSA PRIVATE KEY----- \n\
MIIEogIBAAKCAQEAqczctTh4gWpO1UN4fVpuzFFeHv+a6383zU8p8ydHDjGd14CW \n\
uqs8PHyKdSzLUJlcajGc4qphnk7waiEHwxdEQC6Ie/RyyNkp0kbSNjHBPqQjm54W \n\
qD6+rdwxE0Yw2pnh/J60MPJpbcXvfPnP12hgfEpllBpBdmVLBI8IejdPGKJt6QAd \n\
o5CVRmwB5qKvHJzIKafgJ38JWZ5f9QwAvv7Ks3zm5EntiTu1zlC/LAoToWB94jAD \n\
FvqmcItsXoJQstX1nUQJXJ+Xvp1YliRQ++AOudcZUCZbvW+ZxdeIzO57668t2dQ1 \n\
To9dIcv//qNBpn8HZiOvV2iPzG3RueOQCJ3S+QIDAQABAoIBAHl7gEyHYm/0xwH9 \n\
i67jjHHRpS04YnYqRpo1ESZN66BUD0GnVi+nYylDUSwUKEI59sxjQoNIsx64C3li \n\
uFLzynjA0hUdByJ7fFhdIvhzEHboqDEmIb+tZG6vIs1PYpSDlBNQ2EpMpPFMs9MO \n\
R40tuBo0gAg94Kf8QHe6fa4y8/wCz4CEDNEhPOTDPbvQAiFsH3O8suDussfHp9SQ \n\
WQJ8vEtai2ieZwpyrw/kx2wN29ecaQQLdFsmiD8CWsJ7gC4JDFzYP4Yp+Rpvutef \n\
VdUpf3H0+EjZrGfR0onbPHmIMO7Z5wj9KaCI8DLlobMGo2k6oAyc5Wjs1Fu2Qhwh \n\
JgbKCAECgYEA1gcdDV4d2h0Rxxk+Tt0tVjqS1VkrhOUpGfk9Czfqy3xONioskuzH \n\
51pMwb08hwoiOFyC8O4oLxqTB+Ws+HxHalBAt+9iqXL2Urng1zn8l6oeLO2a2wUy \n\
tjOlrewBM3VnYwUnExHhq4yrqMz7mOGGir5ir8M7wvYs99meur9tPeECgYEAyxli \n\
DTqrkVQRwNthrdtVaGmqTKR5PEM4FPQIXa9+OtYXX2yIGy9r2UV6Gbti4wAzkSCU \n\
WMBqJKpuLxIGCBkniXpTfPBwsaVGuf6KEvUKLsxYjmaNe7tvoWvxyaSrLxcLs8Cx \n\
OalLBRMxyXQWkVCT+cE8NFq5uNRGBFy8JrP7yBkCgYApj1wO2nXFgtuO49H7kmtZ \n\
USpuD1CqD625g3JdGVCYjtzJG8toBF6VCC3beRmlx0v50c0DZ9uZuflPEURTRB7C \n\
AC9xCCUKbm5yhfGpGXN5J+mQI9tsKEg+pSzI5wkcqvGWgsuhollF7JzwvE0m1LRB \n\
gNK8+pZCN3dn8XQnI0DdQQKBgHc8s8AojcmIndOn+LTlbEMcpgrRFQw4OpiynKbD \n\
/Uv1nUs8oLu1H5Azpfetd+TlnWC6y90/OhHErWOdJUVj3z5rPtC/KKpi6h8QPWfb \n\
zNqkxej0dGQMKwGOKinEL2w6D3i8zE+aAJ0+I6CuMwGlWPrsY7go+5hzp5ewChZ+ \n\
3HchAoGAE9Qi5WUVtUDyxmxmH09RQ0Uw0wtG+DKSOLt85PCViumLkXnCqyEfl753 \n\
yVrhK7I15UTYcVb+nUFx4IaaLk1goKs6xgK7O5IobiD7IqXK2HiCMhbyzEtOz4Go \n\
3ueI1Py8CKjsGef4w0ge/q1TKZ8sBlqSJxW0sQkLx0CXS0qwjwk= \n\
-----END RSA PRIVATE KEY----- \n\" > .chef/admin.pem \n\
mkdir -p .chef/trusted_certs \n\
echo -e \"-----BEGIN CERTIFICATE----- \n\
MIIDlDCCAnwCCQDCya3zKdh82jANBgkqhkiG9w0BAQUFADCBizELMAkGA1UEBhMC \n\
VVMxCzAJBgNVBAgMAldBMRAwDgYDVQQHDAdTZWF0dGxlMRAwDgYDVQQKDAdZb3VD \n\
b3JwMRMwEQYDVQQLDApPcGVyYXRpb25zMRYwFAYDVQQDDA00Ni4xMDEuNDEuMjE3 \n\
MR4wHAYJKoZIhvcNAQkBFg95b3VAZXhhbXBsZS5jb20wHhcNMTUwNTI2MTUxMDM4 \n\
WhcNMjUwNTIzMTUxMDM4WjCBizELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAldBMRAw \n\
DgYDVQQHDAdTZWF0dGxlMRAwDgYDVQQKDAdZb3VDb3JwMRMwEQYDVQQLDApPcGVy \n\
YXRpb25zMRYwFAYDVQQDDA00Ni4xMDEuNDEuMjE3MR4wHAYJKoZIhvcNAQkBFg95 \n\
b3VAZXhhbXBsZS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCu \n\
X38V9dWLAYOfHYqkzwhWA7mJarhSMMURTkC8q6I+ZC4dStPbPYy9b302uUI09myU \n\
t5O98w7MFHa9EqXFT7go/KvQ3EPYp4nuF0IjzITxMEIl6/62eVj+yQUHcUf3lIrb \n\
rdyhHXQlTvZrGyYSHWn47vIaWJ/Uui5LR+D7WyXVeBn989HZbOBo9Dk4GC3HExDN \n\
6EYNiD3HYT8fqG4VJWWBHiwkMxO8px9zZsgilgraGHlErk04mcgTd89uMBFbu46z \n\
x0z0tVRWz52DbdJgv6QesnTs1CJHXJ2t8STElLUZgtOjg8pA3I8ePJMCWcbukeSQ \n\
NtEtSFGyTnmH8PiOE4Z3AgMBAAEwDQYJKoZIhvcNAQEFBQADggEBADdvk8SHQY2B \n\
dVuon4IrQ+v+oTcLupcUY8L2C7YQ4F8Yyih3A6rR0omNhq2cAGLZj09rco/PVb+s \n\
umBVFSVJ0EvWvvEcUGQGIsQ8Mo6NPCM2qHzkvEJY5GkGj36NUarkof0qOymW3vQB \n\
Ic42QttaeM0lKUKiCM1MUQd8Wk2ienzSrQIcbuNJqZ/i4C/+UBUDPc/nQwYjvjEg \n\
v4C5ot2lUTMTWy9K/+1GCJ3WMhaoUc4Ulpp5zBsyxtb4bzd0jiLQD1/wltZxB/cY \n\
WhULYlHRPCtZHbbGR4v43UnZ86yJIIL7sT+4GYJ0vhiI5XpCX9dtAzOHINx87GJW \n\
vCqGCrKeboI= \n\
-----END CERTIFICATE----- \n\" > .chef/trusted_certs/46.101.41.217.crt \n\
# Create client config\n\
rm -rf /etc/chef\n\
mkdir -p /etc/chef\n\
echo -e \"log_level                :info \n\
log_location             STDOUT \n\
chef_server_url          \x5C\"$CHEF_SERVER\x5C\" \n\
validation_client_name   \x5C\"chef-validator\x5C\" \n\" > /etc/chef/client.rb \n\
echo -e \"-----BEGIN RSA PRIVATE KEY----- \n\
MIIEpQIBAAKCAQEAv3MybifmzHabaxkV/36/Gk4VzBWTu+AKpYuMNzQvFdzGuer9 \n\
Xo4YWJjSgSQ18gd2k3igEa/0UODlw5KocdVema96Ya6wrWIr4cPwN0++Vc6FNJjB \n\
9wnf5pTkNK+BdG6cIUpUczRmVyF78Zen+2bkOfAZzL1hs+xIQiyyD5lmka72lWPm \n\
v79+2EVTyMPeKRB77AiiWKPpfhoO6Ij0uOUJ+WOAifpyd7kwc2PmXn/AVwS1KVI7 \n\
1oOeNhGoHU5pLraZPLYCQ3Ge/810TQeT92RgGb7h+du9pp7PFmSb/A1u0hiu84x0 \n\
3BqTOPXfaT+qt/zt3nbawGyxLgJUsBLZW2TEUwIDAQABAoIBAQCCmPCT2EBGxbnr \n\
audorGtsZEvWNfnbNU/N8c2qcryT61DczoIcGE5fUZB3a94oxhKLMsPujNrauS/D \n\
I64oSOcJa3zOWb5H6CoLDgA1mv45xnFzmajm/iNwRa0Oi5KBfFqhEvVcySfu0/aa \n\
rVt30Y5qP3E6Cw41ED+SdK7amUgwo7soboTC/igcs71+XEjXcaCz46OSzUBlNu2f \n\
PGd7E54O/ETNUr4zKX6XwBu7ZTVYgnItwt0/3/EDwhmvya+N3Sqd8CmIdulM9W1v \n\
oLv949+kWtcDebT99C2xRC7X8QO3tvIudPzqRNTVnqSqwCTqCjIhS89Uzfx0cWh5 \n\
R9euJEnxAoGBAPukZ1VOfGJ7APNpvdVrXxt4H3VDmIGk0kpXeNmdBu0eLidSsdGv \n\
J5E/gq7RXczyoGq998nmmjyXIi0vaAdX6nn+faMF5TuX8bPSfGxE6AivDzQNgpxz \n\
gSeYwJpAqmGAYr0DKU+C8pmWkm00Nhxc4PETB0Z9ydfpaERKSejaRrjHAoGBAMLD \n\
8gNxxbLP7m9csEI8DuXa/maeF9YZq7W8DkrSyEQfsW1dCLmrpxu+GOEtuiDw1hF/ \n\
rPpp4AIM9Egx5cpYvARVpdPb0h/hxGXTRwC4bi1iZzvdbsisSgMgUGnhl/cKKZsB \n\
d8aPMOPwxecJ5AcfXb9R7JG5yvuco4TGZbJ6J4QVAoGBAIjT3QgKlCGsRlkUjw8j \n\
weQazegiKCOeLmYT5/dTpbd66JejJGMifT5CLVnHJ9zPtKA1yS00QXtepkuxcjxD \n\
9o/86+2fwARGWztu4Q6x7QUDwx2HlxRgzcJtEZUOQlafy5CAzo6YYjdXIylT8XQ8 \n\
oGcU9KjEruLC3Fn9lvCQ91FzAoGAPm3ukloSo8lwxTNxsBluMRFsQFpmi5M47gz0 \n\
i9lapjN+fO64UEOMUiEx+mPc6JgqFfvV54KuchnS551O6fhddAo8GFe618tXV8hM \n\
VxfHATcsngzRaPOI2c2u2PGd82rHgU8MJJJbeoT/+pm6BOpx81SAPwnN3a8M3w91 \n\
I6tyJS0CgYEA0ZJlRvXvMxAIuaYZlXJbneCRLSQzhBUwnWdjbv5W6tzMO4quXUCf \n\
cQgsk3xqwYM5FM0NRdG4ZjNShCNTMCawmB2Ykhh2nUe34i61JzJ95YEbOKeaxqu0 \n\
+3lKpCYPgTHEUD9dIBoWATFNKfxUcyXcon+uusRSd+ulBaatf2EQDHE=  \n\
-----END RSA PRIVATE KEY----- \n\" > /etc/chef/validation.pem \n\
cp /etc/chef/validation.pem .chef/chef-validator.pem \n\
cp -R .chef/trusted_certs /etc/chef/trusted_certs \n\
\n\
# Update SELinux\n\
echo 0 >/selinux/enforce\n\
sed -i 's/=enforcing/=permissive/g' /etc/selinux/config\n\
\n\
# Client configuration\n\
HOSTNAME=\$(hostname -s)
knife client show \$HOSTNAME 1>/dev/null 2>/dev/null
# We use -e echo to workaround CHEF-2702
if [ \$? -eq 100 ]; then
	echo Creating new client
	knife client create \$HOSTNAME -f /etc/chef/client.pem -e echo \n\
else \n\
	echo Re-registering client
	rm -f /etc/chef/client.pem
	knife client reregister \$HOSTNAME -f /etc/chef/client.pem -e echo \n\
fi\n" > $OUTPUT_FILE

chmod +x $OUTPUT_FILE

SSH_USERNAME=root
SUDO=
SSH_PORT=22
SSH_OPTS="-q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR"
SCP_OPTS=$SSH_OPTS

EX_PROMPT="{^.*root@.*[#~] ?$}"

echo "Copying bootstrap to $TARGET ($HOSTNAME)"

ssh -p $SSH_PORT $SSH_KEY $SSH_OPTS $SSH_USERNAME@$HOSTNAME "yum install -y openssh-clients"
if [ $? -eq 0 ]; then
    scp -P $SSH_PORT $SSH_KEY $SCP_OPTS $OUTPUT_FILE $SSH_USERNAME@$HOSTNAME:~/
else
	echo "Couldn't connect using default options; assuming bootstrap of configured host"
	if [ ! -f $DEPLOYMENT_KEY ]; then
		echo "ERROR Couldn't find deployment key: $DEPLOYMENT_KEY; exiting"
		exit 1
	fi

	# Assume we're reconfiguring an existing host
	SUDO=sudo
	SSH_USERNAME=spanner
    SSH_PORT=5123
    SSH_KEY="-i $DEPLOYMENT_KEY"
	chmod 600 $DEPLOYMENT_KEY

    ssh -p $SSH_PORT $SSH_KEY $SSH_OPTS $SSH_USERNAME@$HOSTNAME "$SUDO ~/$OUTPUT_FILE && $SUDO rm -rf ~/$OUTPUT_FILE ~/.chef && $SUDO chef-client -N \$(hostname -s)"
    exit

fi
if [ $? -ne 0 ]; then
	echo "Couldn't copy script to host."
	exit 1
fi

echo "Executing script on $TARGET ($HOSTNAME)"

ssh -p $SSH_PORT $SSH_KEY $SSH_OPTS $SSH_USERNAME@$HOSTNAME "$SUDO ~/$OUTPUT_FILE && $SUDO rm -rf ~/$OUTPUT_FILE ~/.chef && $SUDO chef-client -N \$(hostname -s)"
if [ $? -ne 0 ]; then
	echo "Couldn't execute script on host."
	exit 1
else
	echo "All done."
fi

rm $OUTPUT_FILE
