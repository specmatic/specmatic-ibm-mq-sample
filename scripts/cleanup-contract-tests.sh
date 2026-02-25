#!/usr/bin/env bash
set -euo pipefail

compose_file="docker-compose.test.yaml"

docker compose -f "$compose_file" down -v
