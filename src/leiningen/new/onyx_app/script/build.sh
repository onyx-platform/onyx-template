#!/bin/bash
set -e
lein clean
lein uberjar
{{#docker?}} docker build -t {{app-name}} :0.1.0 . {{/docker?}}
