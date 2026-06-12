package com.kleaves.demo.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortParserTest {

    @Test
    void parseNull_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse(null);
        assertThat(result).isEmpty();
    }

    @Test
    void parseEmpty_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse("");
        assertThat(result).isEmpty();
    }

    @Test
    void parseBlank_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse("   ");
        assertThat(result).isEmpty();
    }

    @Test
    void parseSingleAscending_shouldReturnOneOrder() {
        List<SortParser.SortOrder> result = SortParser.parse("price");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isTrue();
    }

    @Test
    void parseSingleDescending_shouldReturnDescending() {
        List<SortParser.SortOrder> result = SortParser.parse("-price");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isFalse();
    }

    @Test
    void parseMultipleMixed_shouldReturnAll() {
        List<SortParser.SortOrder> result = SortParser.parse("author,-price");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("author");
        assertThat(result.get(0).ascending()).isTrue();
        assertThat(result.get(1).field()).isEqualTo("price");
        assertThat(result.get(1).ascending()).isFalse();
    }

    @Test
    void parseInvalidField_shouldBeIgnored() {
        List<SortParser.SortOrder> result = SortParser.parse("invalidField");

        assertThat(result).isEmpty();
    }

    @Test
    void parseMixedValidAndInvalid_shouldKeepOnlyValid() {
        List<SortParser.SortOrder> result = SortParser.parse("price,invalid,author");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(1).field()).isEqualTo("author");
    }

    @Test
    void parseWithSpaces_shouldTrimFields() {
        List<SortParser.SortOrder> result = SortParser.parse(" price , -author ");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isTrue();
        assertThat(result.get(1).field()).isEqualTo("author");
        assertThat(result.get(1).ascending()).isFalse();
    }

    @Test
    void parseAllThreeValidFields() {
        List<SortParser.SortOrder> result = SortParser.parse("title,author,price");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).field()).isEqualTo("title");
        assertThat(result.get(1).field()).isEqualTo("author");
        assertThat(result.get(2).field()).isEqualTo("price");
    }
}
