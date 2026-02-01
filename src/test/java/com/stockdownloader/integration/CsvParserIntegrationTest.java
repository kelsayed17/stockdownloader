package com.stockdownloader.integration;

import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.CsvParser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the CSV parsing pipeline: raw CSV data -> CsvParser -> CsvPriceDataLoader -> PriceData.
 * Tests the interaction between the parser and the data loader.
 */
class CsvParserIntegrationTest {

    @Test
    void parseCsvToPriceDataEndToEnd() {
        String csv = """
                Date,Open,High,Low,Close,Adj Close,Volume
                2023-01-03,384.37,386.43,377.83,380.82,380.82,74850700
                2023-01-04,383.18,385.88,380.00,383.76,383.76,68860700
                2023-01-05,381.72,381.84,378.76,379.38,379.38,57510600
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(3, data.size());

        PriceData first = data.get(0);
        assertEquals("2023-01-03", first.date());
        assertEquals(new BigDecimal("384.37"), first.open());
        assertEquals(new BigDecimal("386.43"), first.high());
        assertEquals(new BigDecimal("377.83"), first.low());
        assertEquals(new BigDecimal("380.82"), first.close());
        assertEquals(new BigDecimal("380.82"), first.adjClose());
        assertEquals(74850700L, first.volume());
    }

    @Test
    void csvParserHandlesQuotedFields() throws IOException {
        String csv = """
                Name,Value
                "Hello, World",42
                "She said \""hi\""",99
                """;

        try (var parser = new CsvParser(new StringReader(csv))) {
            String[] header = parser.readNext();
            assertArrayEquals(new String[]{"Name", "Value"}, header);

            String[] row1 = parser.readNext();
            assertEquals("Hello, World", row1[0]);
            assertEquals("42", row1[1]);

            String[] row2 = parser.readNext();
            assertEquals("She said \"hi\"", row2[0]);
            assertEquals("99", row2[1]);
        }
    }

    @Test
    void csvParserWithCustomDelimiter() throws IOException {
        String tsv = "Name\tValue\nAlice\t100\nBob\t200\n";

        try (var parser = new CsvParser(new StringReader(tsv), '\t')) {
            String[] header = parser.readNext();
            assertEquals("Name", header[0]);
            assertEquals("Value", header[1]);

            String[] row1 = parser.readNext();
            assertEquals("Alice", row1[0]);
            assertEquals("100", row1[1]);

            String[] row2 = parser.readNext();
            assertEquals("Bob", row2[0]);
            assertEquals("200", row2[1]);
        }
    }

    @Test
    void csvParserReadAllIntegration() throws IOException {
        String csv = """
                A,B,C
                1,2,3
                4,5,6
                7,8,9
                """;

        try (var parser = new CsvParser(new StringReader(csv))) {
            List<String[]> all = parser.readAll();
            assertEquals(4, all.size()); // header + 3 rows
            assertArrayEquals(new String[]{"A", "B", "C"}, all.get(0));
            assertArrayEquals(new String[]{"1", "2", "3"}, all.get(1));
            assertArrayEquals(new String[]{"7", "8", "9"}, all.get(3));
        }
    }

    @Test
    void priceDataLoaderSkipsInvalidRows() {
        String csv = """
                Date,Open,High,Low,Close,Adj Close,Volume
                2023-01-03,384.37,386.43,377.83,380.82,380.82,74850700
                2023-01-04,null,null,null,null,null,0
                2023-01-05,381.72,381.84,378.76,379.38,379.38,57510600
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(2, data.size(), "Invalid rows should be skipped");
        assertEquals("2023-01-03", data.get(0).date());
        assertEquals("2023-01-05", data.get(1).date());
    }

    @Test
    void priceDataLoaderHandlesMinimalCsv() {
        String csv = """
                Date,Open,High,Low,Close
                2023-01-03,100,110,90,105
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(1, data.size());
        PriceData row = data.getFirst();
        assertEquals(new BigDecimal("105"), row.close());
        assertEquals(new BigDecimal("105"), row.adjClose(), "Missing adjClose should default to close");
        assertEquals(0L, row.volume(), "Missing volume should default to 0");
    }

    @Test
    void priceDataLoaderFromResourceFile() {
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(
                CsvParserIntegrationTest.class.getResourceAsStream("/test-price-data.csv"));

        assertFalse(data.isEmpty());
        assertTrue(data.size() > 100, "Resource file should have substantial data");

        // Verify data integrity across all rows
        for (PriceData bar : data) {
            assertNotNull(bar.date());
            assertTrue(bar.high().compareTo(bar.low()) >= 0,
                    "High should be >= Low for " + bar.date());
            assertTrue(bar.high().compareTo(bar.open()) >= 0,
                    "High should be >= Open for " + bar.date());
            assertTrue(bar.high().compareTo(bar.close()) >= 0,
                    "High should be >= Close for " + bar.date());
            assertTrue(bar.low().compareTo(bar.open()) <= 0,
                    "Low should be <= Open for " + bar.date());
            assertTrue(bar.low().compareTo(bar.close()) <= 0,
                    "Low should be <= Close for " + bar.date());
        }
    }

    @Test
    void emptyStreamReturnsEmptyList() {
        String csv = "Date,Open,High,Low,Close,Adj Close,Volume\n";
        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);
        assertTrue(data.isEmpty(), "Empty CSV (header only) should return empty list");
    }

    @Test
    void csvParserSkipLinesIntegration() throws IOException {
        String csv = "Comment line 1\nComment line 2\nA,B\n1,2\n";

        try (var parser = new CsvParser(new StringReader(csv))) {
            parser.skipLines(2);
            String[] header = parser.readNext();
            assertArrayEquals(new String[]{"A", "B"}, header);

            String[] data = parser.readNext();
            assertArrayEquals(new String[]{"1", "2"}, data);
        }
    }
}
