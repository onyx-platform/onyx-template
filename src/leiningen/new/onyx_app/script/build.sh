#!/bin/bash
set -e
lein clean
lein uberjar
{{#docker?}}docker build -f script/kafka-meetup-streamer/Dockerfile -t {{app-name}}/kafka-meetup-streamer:0.1.0 . {{/docker?}}
{{#docker?}}docker build -t {{app-name}}:0.1.0 . {{/docker?}}
