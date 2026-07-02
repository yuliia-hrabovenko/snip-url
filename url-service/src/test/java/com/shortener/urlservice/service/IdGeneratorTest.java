package com.shortener.urlservice.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGeneratorTest {

    @Test
    void generatesUniqueMonotonicallyNonDecreasingIds() {
        IdGenerator generator = new IdGenerator(0, 0, 5);
        Set<Long> seen = new HashSet<>();
        long previous = -1L;
        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId();
            assertTrue(id > previous, "ids must be strictly increasing within a single generator");
            assertTrue(seen.add(id), "ids must be unique");
            previous = id;
        }
    }

    @Test
    void idsFromDifferentWorkersNeverCollide() {
        IdGenerator worker0 = new IdGenerator(0, 0, 5);
        IdGenerator worker1 = new IdGenerator(0, 1, 5);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(ids.add(worker0.nextId()));
            assertTrue(ids.add(worker1.nextId()));
        }
    }

    @Test
    void rejectsOutOfRangeDatacenterId() {
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(32, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(-1, 0, 5));
    }

    @Test
    void rejectsOutOfRangeWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(0, 32, 5));
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(0, -1, 5));
    }

    @Test
    void acceptsBoundaryDatacenterAndWorkerIds() {
        IdGenerator generator = new IdGenerator(31, 31, 5);
        assertTrue(generator.nextId() > 0);
    }
}
