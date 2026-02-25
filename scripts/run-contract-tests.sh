#!/usr/bin/env bash
set -euo pipefail

compose_file="docker-compose.test.yaml"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose is required" >&2
  exit 1
fi

exit_code=0

# Start the stack in the background

docker compose -f "$compose_file" up -d

# Stream only Specmatic logs live

docker compose -f "$compose_file" logs -f specmatic-test &
log_pid=$!

# Wait for the Specmatic container to exit and capture its exit code
set +e
specmatic_cid=$(docker compose -f "$compose_file" ps -q specmatic-test)
exit_code=$(docker wait "$specmatic_cid")
set -e

# Stop log streaming
kill "$log_pid" >/dev/null 2>&1 || true
wait "$log_pid" 2>/dev/null || true

if [[ "${SHOW_ALL_LOGS:-}" == "1" ]]; then
  docker compose -f "$compose_file" logs --no-color specmatic-test app ibm-mq || true
fi

# Leave containers running for debugging; use cleanup script to tear down.

exit "$exit_code"
