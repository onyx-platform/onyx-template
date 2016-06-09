#!/bin/sh

/opt/jdk/bin/java -cp /opt/peer.jar {{app-name}}.core start-peers "$NPEERS" -p :docker
