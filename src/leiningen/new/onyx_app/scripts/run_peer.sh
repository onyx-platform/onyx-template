#!/bin/sh

/usr/bin/java -cp /opt/peer.jar {{app-name-underscore}}.core start-peers "$NPEERS" -p :docker
