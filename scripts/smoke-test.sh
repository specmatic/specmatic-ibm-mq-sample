#!/usr/bin/env sh
set -eu

./gradlew bootRun --args="--app.smoke-test.enabled=true --spring.jms.listener.auto-startup=false"
