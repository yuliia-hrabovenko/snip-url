#!/bin/bash
set -e

URI="mongodb://appuser:apppass@mongo-router:27017/admin?authSource=admin"

echo "[mongo-setup] waiting for mongo-router to accept authenticated connections..."
until mongosh "$URI" --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
  sleep 2
done
echo "[mongo-setup] mongo-router is reachable."

# addShard needs each target shard's replica set to have an elected PRIMARY, which can lag
# slightly behind mongo-router becoming reachable - retry the whole script a few times
# rather than failing on a transient "no primary yet" error.
ATTEMPTS=0
MAX_ATTEMPTS=15
until mongosh "$URI" --quiet /mongo-scripts/init-shards-and-sharding.js; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo "[mongo-setup] giving up after $MAX_ATTEMPTS attempts."
    exit 1
  fi
  echo "[mongo-setup] setup script failed, retrying in 5s (attempt $ATTEMPTS/$MAX_ATTEMPTS)..."
  sleep 5
done

echo "[mongo-setup] done."
