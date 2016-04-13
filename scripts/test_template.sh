#!/bin/bash
lein set-version 99.99.99
lein install
rm -rf test-app-docker-metrics
lein new onyx-app test-app-docker-metrics -- +docker +metrics
cd test-app-docker-metrics && lein test && cd ..
rm -rf test-app-docker-metrics
git checkout project.clj
rm -rf ~/.m2/repository/onyx-app/lein-template/99.99.99/
