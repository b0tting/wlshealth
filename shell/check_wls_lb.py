#!/usr/bin/python
#############################################################################
# check_osb_lb is a Nagios check script for the state of a F5
# load balancer health check.
#
# Note the static dictionary containing load balancer address mapping
# to OSB clusters
#
# @author MarkO
#
#############################################################################

import os
import sys
import urllib2
import argparse
from osb_environment_variables import domainConfig

serverPort = "8011"
loadBalancerDict = {
    "lb_config_1": ("wlsserver1","wlsserver2"),
    "lb_config_2": ("wlsprodserver1","wlsprodserver2"),
}


STATE_OKAY = 0
STATE_SHUTDOWN_BY_US = 1
STATE_SHUTDOWN_BY_SERVER = 2
STATE_NOT_REACHEABLE = 3
STATE_NOT_FOUND = 4

NAGIOS_OK = 0
NAGIOS_WARNING = 1
NAGIOS_CRITICAL = 2
NAGIOS_UNKNOWN = 3
NAGIOS_DICT = {NAGIOS_OK: "OK", NAGIOS_WARNING: "WARNING", NAGIOS_CRITICAL: "CRITICAL", NAGIOS_UNKNOWN: "UNKNOWN"}
f5MonitorURL = "/zzwlshealth/index.jsp"

def testServerState(server):
    req = urllib2.Request("http://" + server + ":" + serverPort + f5MonitorURL)
    result = STATE_OKAY
    try:
        urllib2.urlopen(req)
    except urllib2.HTTPError as e:
        if e.code == 503:
            if "Load balancer, please do not send me messages. " in e.read():
                result = STATE_SHUTDOWN_BY_US
            else:
                result = STATE_SHUTDOWN_BY_SERVER
        elif e.code == 404:
            result = STATE_NOT_FOUND
        else:
            result = STATE_NOT_REACHEABLE
    except urllib2.URLError:
        result = STATE_NOT_REACHEABLE
    return result


description = '''
This is a Nagios check script for the "zzwlshealth" health check, used by the F5 load balancer and WebLogic to 
see which WebLogic instance can receive requests. This script returns a valid Nagios exit code and sensible message.  
'''
parser = argparse.ArgumentParser(description=description)
parser.add_argument("-c", "--cluster", help="Load balancer config to check (" + ",".join(loadBalancerDict.keys()) + ")")
args = parser.parse_args()

if(not args.cluster or args.cluster not in loadBalancerDict):
    parser.print_help()
    sys.exit(NAGIOS_UNKNOWN)

clusterTuple = loadBalancerDict[args.cluster]
nagios = NAGIOS_OK
nagiosString = "F5 load balancer health check is okay, servers should be accepting traffic"

for system in clusterTuple:
    result = testServerState(system)
    if result != STATE_OKAY:
        if nagios == NAGIOS_OK:
            nagios = NAGIOS_WARNING
        elif nagios == NAGIOS_WARNING:
            nagios = NAGIOS_CRITICAL

        if result == STATE_SHUTDOWN_BY_SERVER:
            nagiosString = args.cluster + " F5 load balancer health check is not okay, weblogic server at " + system + " is probably shutting down."
        elif result == STATE_SHUTDOWN_BY_US:
            nagiosString = args.cluster + " F5 load balancer health check is not okay, check at " + system + " was disabled by hand."
        elif result == STATE_NOT_FOUND:
            nagios = NAGIOS_UNKNOWN
            nagiosString = system + " F5 load balancer health check was not found! Please deploy first."
        elif result == STATE_NOT_REACHEABLE:
            nagiosString = args.cluster + " F5 load balancer health check is not okay, weblogic server at " + system + " could not be reached."
nagiosString = NAGIOS_DICT[nagios] + ": " + nagiosString
print(nagiosString)
sys.exit(nagios)




