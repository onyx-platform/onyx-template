#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

export BIND_ADDR="${BIND_ADDR:-$(hostname --ip-address)}"
export APP_NAME=$(echo "{{app-name}}" | sed s/"-"/"_"/g)
exec java ${PEER_JAVA_OPTS:-} -cp /srv/{{app-name}}.jar "$APP_NAME.launcher.launch_prod_peers" $NPEERS {{^docker?}}>>/var/log/onyx.log 2>&1{{/docker?}}
