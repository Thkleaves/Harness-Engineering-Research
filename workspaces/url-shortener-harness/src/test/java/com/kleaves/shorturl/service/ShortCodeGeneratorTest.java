package com.kleaves.shorturl.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new RandomShortCodeGenerator();

    @Test
    void shouldGenerate7CharacterCode() {
        String code = generator.generate();
        assertEquals(7, code.length());
    }

    @Test
    void shouldOnlyContainValidCharacters() {
        String code = generator.generate();
        assertTrue(code.matches("[a-zA-Z0-9]{7}"));
    }

    @Test
    void shouldBeCaseSensitiveDistinct() {
        // Generate 200 codes and verify we see both upper and lower case chars
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            for (char c : generator.generate().toCharArray()) {
                seen.add(c);
            }
        }
        boolean hasUpper = seen.stream().anyMatch(Character::isUpperCase);
        boolean hasLower = seen.stream().anyMatch(Character::isLowerCase);
        assertTrue(hasUpper, "Should contain uppercase letters");
        assertTrue(hasLower, "Should contain lowercase letters");
    }

    @Test
    void shouldGenerateUniqueCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        assertEquals(1000, codes.size(), "All 1000 codes should be unique");
    }
}
