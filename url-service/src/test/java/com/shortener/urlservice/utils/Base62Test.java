package com.shortener.urlservice.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62Test {

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 61L, 62L, 123456789L, Long.MAX_VALUE, 9007199254740993L})
    void encodeDecodeRoundTrips(long value) {
        assertEquals(value, Base62.decode(Base62.encode(value)));
    }

    @Test
    void encodeZeroReturnsFirstAlphabetCharacter() {
        assertEquals("0", Base62.encode(0L));
    }

    @Test
    void decodeRejectsInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc!"));
    }

    @Test
    void decodeRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(""));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(null));
    }
}
