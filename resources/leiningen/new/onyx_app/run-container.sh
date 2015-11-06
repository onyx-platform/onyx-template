#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

if [[ "$#" -ne 2 ]]; then
    echo "Usage: $0 onyx-id n-peers"
    echo "Example: $0 3424312384i3423 4"
    exit 1
fi

ONYX_ID=$1
NPEERS=$2

docker run --privileged -e ONYX_ID=$ONYX_ID -e NPEERS=$NPEERS -d {{app-name}}:0.1.0
