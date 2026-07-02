#!/bin/bash
set -e

mongod --configsvr --replSet configReplSet --keyFile /etc/mongo-keyfile --bind_ip_all --port 27017 &
MONGOD_PID=$!

echo "[init-configsvr] waiting for mongod to accept connections..."
until mongosh --quiet --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
  sleep 1
done
echo "[init-configsvr] mongod is up."

AUTH_ARGS=(-u appuser -p apppass --authenticationDatabase admin)
if ! mongosh --quiet "${AUTH_ARGS[@]}" --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
  AUTH_ARGS=()
fi

if ! mongosh --quiet "${AUTH_ARGS[@]}" --eval "rs.status().ok" 2>/dev/null | grep -q '^1$'; then
  echo "[init-configsvr] initiating configReplSet..."
  mongosh --quiet "${AUTH_ARGS[@]}" --eval '
    rs.initiate({
      _id: "configReplSet",
      configsvr: true,
      members: [{ _id: 0, host: "mongo-config:27017" }]
    })
  '
fi

echo "[init-configsvr] waiting for PRIMARY election..."
until mongosh --quiet "${AUTH_ARGS[@]}" --eval "db.hello().isWritablePrimary" 2>/dev/null | grep -q '^true$'; do
  sleep 1
done
echo "[init-configsvr] configReplSet has a PRIMARY."

if [ ${#AUTH_ARGS[@]} -eq 0 ]; then
  USER_COUNT=$(mongosh --quiet --eval 'db.getSiblingDB("admin").system.users.countDocuments({ user: "appuser" })' 2>/dev/null | tail -1)
  if [ "$USER_COUNT" != "1" ]; then
    echo "[init-configsvr] creating admin user..."
    mongosh --quiet --eval '
      db.getSiblingDB("admin").createUser({
        user: "appuser",
        pwd: "apppass",
        roles: [{ role: "root", db: "admin" }]
      })
    '
  fi
fi

echo "[init-configsvr] ready."
wait $MONGOD_PID
