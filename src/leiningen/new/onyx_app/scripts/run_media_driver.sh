#!/bin/bash
CGROUPS_MEM=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
MEMINFO_MEM=$(($(awk '/MemTotal/ {print $2}' /proc/meminfo)*1024))
MEM=$(($MEMINFO_MEM>$CGROUPS_MEM?$CGROUPS_MEM:$MEMINFO_MEM))
JVM_MEDIA_DRIVER_HEAP_RATIO=${JVM_MEDIA_DRIVER_HEAP_RATIO:-0.2}
XMX=$(awk '{printf("%d",$1*$2/1024^2)}' <<< " ${MEM} ${JVM_MEDIA_DRIVER_HEAP_RATIO} ")
# Use the container memory limit to set max heap size so that the GC
# knows to collect before it's hard-stopped by the container environment,
# causing OOM exception.

: ${MEDIA_DRIVER_JAVA_OPTS:='-server'}

/usr/bin/java $MEDIA_DRIVER_JAVA_OPTS \
              "-Xmx${XMX}m" \
              -cp /opt/peer.jar \
              lib_onyx.media_driver
