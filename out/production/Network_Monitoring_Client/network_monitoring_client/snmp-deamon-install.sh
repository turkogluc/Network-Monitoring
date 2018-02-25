#!/bin/bash

echo "yes" | sudo apt-get install snmp
echo "yes" | sudo apt-get install snmp

sudo sed 's/agentAddress/##/g'
old=$(cat /etc/snmp/snmpd.conf)

printf "$old\nview systemonly included .1\nview systemonly included system\n agentAddress udp:161,udp6:[::1]:161" > /etc/snmp/snmpd.conf

