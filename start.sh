#!/bin/bash

set -e

check_port () {
  PORT=$1
  PIDS=$(lsof -ti tcp:$PORT 2>/dev/null | tr '\n' ' ')
  if [ -n "$PIDS" ]; then
    echo "WARNING: Port $PORT is already in use by PID(s): $PIDS"
    echo "Make sure this is intentional (Docker is already running)"
  else
    echo "Port $PORT is free."
  fi
}

check_port 5432
check_port 8080
check_port 4200

echo "All required ports are free."
echo "--------------------------------"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker is not installed or not in PATH."
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "ERROR: docker compose is not available."
  exit 1
fi

echo "Building and starting Docker Compose services..."
docker compose up -d --build

echo "--------------------------------"
echo "Docker containers are starting..."
echo ""
echo "Postgres:     localhost:5432"
echo "Backend:      http://localhost:8080"
echo "Frontend:     http://localhost:4200"
echo ""
echo "--------------------------------"

docker compose logs -f