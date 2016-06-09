#!/bin/sh

/opt/jdk/bin/java -cp /opt/peer.jar {{app-name-underscore}}.core start-peers "$NPEERS" -p :docker
