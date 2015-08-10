#!/usr/bin/perl -w
# 
# check_network_bandwidth.pl
# by mlukasik 24 Feb 2012
#
# Calculates current bandwidth consumption based on the last 10 seconds
# worth of data, read from /proc/net/dev

use strict;
use Getopt::Std;

my %interface; # hash for the interface

my $rxdata = 0;
my $txdata = 0;
my $rxpackets = 0;
my $txpackets = 0;
my $totaldata = 0;
my $totalpackets = 0;

my $output = 0; # output data returned by this script
my $parsedOutput = ""; # nicely parsed string with data for Nagios

my $sleepTime = 5; # averaging time (time in secs between two samples are taken)

my $usage = "Usage: $0 [-i interface] [-w value] [-c value]\
\t-i  interface name (e.g. eth0)\
\t-w  warning value for all 6 data types, comma-separated (e.g. 10,10,10,10,10,10)\
\t-c  critical value for all 6 data types, comma-separated (e.g. 100,100,100,100,100,100)\
\
";


die "ERROR: All the arguments have to be specified\n", $usage if $#ARGV != 5;
die $usage, if !getopts("i:w:c:", \my %opts);



	
open FILE, "/proc/net/dev" or die "Unable to open /proc/net/dev: $!";

foreach (<FILE>) {

	next if $_ !~ /$opts{'i'}\:/;
	
	($interface{$opts{'i'}}{'rxdata'}, $interface{$opts{'i'}}{'rxpackets'}, $interface{$opts{'i'}}{'txdata'}, $interface{$opts{'i'}}{'txpackets'}) =
	($_ =~ /\s?eth\d+\:\s*(\d+)\s+(\d+)\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+(\d+)\s+(\d+)\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+$/gi);

}

close FILE;


sleep $sleepTime;


open FILE, "/proc/net/dev" or die "Unable to open /proc/net/dev: $!";

foreach (<FILE>) {

	next if $_ !~ /$opts{'i'}\:/;

	($interface{$opts{'i'}}{'rxdatanew'}, $interface{$opts{'i'}}{'rxpacketsnew'}, $interface{$opts{'i'}}{'txdatanew'}, $interface{$opts{'i'}}{'txpacketsnew'}) =
	($_ =~ /\s?eth\d+\:\s*(\d+)\s+(\d+)\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+(\d+)\s+(\d+)\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+\s+\d+$/gi);

}

close FILE;



$rxdata = ($interface{$opts{'i'}}{'rxdatanew'} - $interface{$opts{'i'}}{'rxdata'}) / $sleepTime;
$txdata = ($interface{$opts{'i'}}{'txdatanew'} - $interface{$opts{'i'}}{'txdata'}) / $sleepTime;
$totaldata = $rxdata + $txdata;

$rxpackets = ($interface{$opts{'i'}}{'rxpacketsnew'} - $interface{$opts{'i'}}{'rxpackets'}) / $sleepTime;
$txpackets = ($interface{$opts{'i'}}{'txpacketsnew'} - $interface{$opts{'i'}}{'txpackets'}) / $sleepTime;
$totalpackets = $rxpackets + $txpackets;



my ($rxdata_warning, $txdata_warning, $totaldata_warning, $rxpackets_warning, $txpackets_warning, $totalpackets_warning) = split(/\,/, $opts{'w'});
my ($rxdata_critical, $txdata_critical, $totaldata_critical, $rxpackets_critical, $txpackets_critical, $totalpackets_critical) = split(/\,/, $opts{'c'});




# Output stuff to nagios

$parsedOutput = "Network checks for $opts{'i'} | " . 
"'RX bytes/s'=$rxdata;$rxdata_warning;$rxdata_critical;0; " . 
"'TX bytes/s'=$txdata;$txdata_warning;$txdata_critical;0; " . 
"'TOTAL bytes/s'=$totaldata;$totaldata_warning;$totaldata_critical;0; " . 

"'RX bits/s'=". $rxdata*8 .";". $rxdata_warning*8 .";". $rxdata_critical*8 .";0; " . 
"'TX bits/s'=". $txdata*8 .";". $txdata_warning*8 .";". $txdata_critical*8 .";0; " . 
"'TOTAL bits/s'=". $totaldata*8 .";". $totaldata_warning*8 .";". $totaldata_critical*8 .";0; " . 


"'RX packets/s'=$rxpackets;$rxpackets_warning;$rxpackets_critical;0; " . 
"'TX packets/s'=$txpackets;$txpackets_warning;$txpackets_critical;0; " . 
"'TOTAL packets/s'=$totalpackets;$totalpackets_warning;$totalpackets_critical;0; ";

if(length($parsedOutput) >= 1024) {
	print "Output string is too long for Nagios to handle, you probably need to edit check_network_bandwidth.pl :(";
	exit 2;
}


print $parsedOutput;


if(($rxdata < $rxdata_warning) or ($txdata < $txdata_warning) || ($totaldata < $totaldata_warning)) {
	exit 0; # OK
} elsif((($rxdata >= $rxdata_warning) or ($txdata >= $txdata_warning) or ($totaldata >= $totaldata_warning)) and
		(($rxdata < $rxdata_critical) or ($txdata < $txdata_critical) or ($totaldata < $totaldata_critical))) {
	exit 1; # WARNING
} elsif(($rxdata >= $rxdata_critical) or ($txdata >= $txdata_critical) or ($totaldata >= $totaldata_critical)) {
	exit 2; # CRITICAL
} else {
	exit -1; # UNKNOWN -- this shouldn't happen!
}
