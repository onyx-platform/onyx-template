#!/bin/bash
set -e
lein clean
lein uberjar
docker build -t {{app-name}}:0.1.0 .
