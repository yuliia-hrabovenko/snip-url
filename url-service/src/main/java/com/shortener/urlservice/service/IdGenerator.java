package com.shortener.urlservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Twitter Snowflake-style 64-bit ID generator. Generates IDs entirely in-process
 * (timestamp + datacenter id + worker id + per-millisecond sequence).
 *
 * Layout (MSB to LSB): 1 unused sign bit | 41 bit timestamp | 5 bit datacenter id |
 * 5 bit worker id | 12 bit sequence.
 *
 * Datacenter/worker ids are not auto-deconflicted across nodes -
 * operators must assign distinct SNOWFLAKE_WORKER_ID per
 * instance and SNOWFLAKE_DATACENTER_ID per region/AZ.
 */
@Service
public class IdGenerator {

    private static final long EPOCH = 1735689600000L;

    private static final int DATACENTER_ID_BITS = 5;
    private static final int WORKER_ID_BITS = 5;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);

    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final int DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long workerId;
    private final long maxBackwardDriftMs;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public IdGenerator(
            @Value("${snowflake.datacenter-id}") long datacenterId,
            @Value("${snowflake.worker-id}") long workerId,
            @Value("${snowflake.max-backward-drift-ms}") long maxBackwardDriftMs) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.maxBackwardDriftMs = maxBackwardDriftMs;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= maxBackwardDriftMs) {
                try {
                    Thread.sleep(offset);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during clock-drift wait", e);
                }
                timestamp = currentTimeMillis();
            } else {
                throw new IllegalStateException("Clock moved backwards by " + offset + "ms, refusing to generate id");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
