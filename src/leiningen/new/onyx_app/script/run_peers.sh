#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

APP_NAME=$(echo "{{app-name}}" | sed s/"-"/"_"/g)
exec /sbin/setuser onyx java -cp /src/{{app-name}}.jar "$APP_NAME.launcher.launch_prod_peers" $ONYX_ID $NPEERS >>/var/log/onyx.log 2>&1
