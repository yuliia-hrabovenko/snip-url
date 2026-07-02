#!/bin/bash
set -e

: "${SHARD_REPLSET_NAME:?SHARD_REPLSET_NAME must be set}"
: "${SHARD_HOST:?SHARD_HOST must be set}"

mongod --shardsvr --replSet "$SHARD_REPLSET_NAME" --keyFile /etc/mongo-keyfile --bind_ip_all --port 27017 &
MONGOD_PID=$!

echo "[init-shard:$SHARD_REPLSET_NAME] waiting for mongod to accept connections..."
until mongosh --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
  sleep 1
done
echo "[init-shard:$SHARD_REPLSET_NAME] mongod is up."

AUTH_ARGS=(-u appuser -p apppass --authenticationDatabase admin)
if ! mongosh --quiet "${AUTH_ARGS[@]}" --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
  AUTH_ARGS=()
fi

if ! mongosh --quiet "${AUTH_ARGS[@]}" --eval "rs.status().ok" 2>/dev/null | grep -q '^1$'; then
  echo "[init-shard:$SHARD_REPLSET_NAME] initiating replica set..."
  mongosh --quiet "${AUTH_ARGS[@]}" --eval "
    rs.initiate({
      _id: '$SHARD_REPLSET_NAME',
      members: [{ _id: 0, host: '$SHARD_HOST:27017' }]
    })
  "
fi

echo "[init-shard:$SHARD_REPLSET_NAME] waiting for PRIMARY election..."
until mongosh --quiet "${AUTH_ARGS[@]}" --eval "db.hello().isWritablePrimary" 2>/dev/null | grep -q '^true$'; do
  sleep 1
done

echo "[init-shard:$SHARD_REPLSET_NAME] ready."
wait $MONGOD_PID
