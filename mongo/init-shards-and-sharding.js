function addShardIfNeeded(connString) {
  try {
    sh.addShard(connString);
    print(`[mongo-setup] addShard ${connString}: ok`);
  } catch (e) {
    print(`[mongo-setup] addShard ${connString}: ${e.message} (assuming already added)`);
  }
}

addShardIfNeeded("shard1ReplSet/mongo-shard1:27017");
addShardIfNeeded("shard2ReplSet/mongo-shard2:27017");
addShardIfNeeded("shard3ReplSet/mongo-shard3:27017");

try {
  sh.enableSharding("appdb");
  print("[mongo-setup] enableSharding appdb: ok");
} catch (e) {
  print(`[mongo-setup] enableSharding appdb: ${e.message} (assuming already enabled)`);
}

try {
  db.getSiblingDB("appdb").short_urls.createIndex({ short_code: 1 }, { unique: true });
  print("[mongo-setup] createIndex short_code (unique): ok");
} catch (e) {
  print(`[mongo-setup] createIndex short_code (unique): ${e.message} (assuming already present)`);
}

try {
  sh.shardCollection("appdb.short_urls", { short_code: "hashed" });
  print("[mongo-setup] shardCollection appdb.short_urls: ok");
} catch (e) {
  if (/already shard/i.test(e.message || "")) {
    print(`[mongo-setup] shardCollection appdb.short_urls: already sharded`);
  } else {
    throw e;
  }
}

print("[mongo-setup] cluster wiring complete.");
