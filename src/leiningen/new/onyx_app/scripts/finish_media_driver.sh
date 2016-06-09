#!/usr/bin/execlineb -S0
# Cleanup after the media driver
s6-svscanctl -t /var/run/s6/services
