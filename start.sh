#!/bin/bash

kill_port () {
  PORT=$1
  PIDS=$(lsof -ti tcp:$PORT 2>/dev/null | tr '\n' ' ')
  if [ -n "$PIDS" ]; then
    echo "Port $PORT is in use by PID(s) $PIDS - stopping..."
    kill $PIDS 2>/dev/null || true
    sleep 1
    kill -9 $PIDS 2>/dev/null || true
  else
    echo "Port $PORT is free."
  fi
}

kill_port 5432
kill_port 8080
kill_port 4200

echo "All required ports are free."
echo "--------------------------------"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

if [ -x "$SCRIPT_DIR/cloud-sql-proxy" ]; then
  CLOUD_SQL_PROXY="$SCRIPT_DIR/cloud-sql-proxy"
elif command -v cloud-sql-proxy >/dev/null 2>&1; then
  CLOUD_SQL_PROXY="$(command -v cloud-sql-proxy)"
else
  echo "ERROR: cloud-sql-proxy not found."
  echo "Put the binary next to start.sh as './cloud-sql-proxy' OR install it and ensure it's on PATH."
  exit 1
fi

echo "Starting Cloud SQL Auth Proxy..."
"$CLOUD_SQL_PROXY" --port 5432 korhazi-munkabeosztas:europe-central2:korhazi-beosztas &
PROXY_PID=$!

for i in {1..15}; do
  if lsof -nP -iTCP:5432 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Cloud SQL Auth Proxy is listening on localhost:5432"
    break
  fi
  sleep 1
done

if ! lsof -nP -iTCP:5432 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "ERROR: Cloud SQL Auth Proxy did not start listening on port 5432."
  echo "Check 'gcloud auth application-default login' and that the instance connection name is correct."
  exit 1
fi

echo "Starting backend (Quarkus)..."
cd backend || exit 1

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

./mvnw quarkus:dev &
BACKEND_PID=$!

cd ..

echo "Starting frontend (Angular)..."
cd frontend || exit 1

npm install
npm start &
FRONTEND_PID=$!

cd ..

echo "Cloud SQL Proxy PID: $PROXY_PID"
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "--------------------------------"
echo "Cloud SQL Proxy: localhost:5432"
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:4200"
echo "--------------------------------"

wait