#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

BIND_ADDR=$(hostname --ip-address)
APP_NAME=$(echo "{{app-name}}" | sed s/"-"/"_"/g)
exec java -cp /srv/{{app-name}}.jar "$APP_NAME.launcher.launch_prod_peers" $ONYX_ID $NPEERS {{^docker?}}>>/var/log/onyx.log 2>&1{{/docker?}}
