#!/bin/bash
lein set-version 99.99.99
lein install
rm -rf test-app-docker
lein new onyx-app test-app-docker -- +docker
cd test-app-docker && lein test && cd ..
rm -rf test-app-docker
git checkout project.clj
rm -rf ~/.m2/repository/onyx-app/lein-template/99.99.99/
