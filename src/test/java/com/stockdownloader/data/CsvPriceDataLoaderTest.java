package com.stockdownloader.data;

import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvPriceDataLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromStream() {
        String csv = """
                Date,Open,High,Low,Close,Adj Close,Volume
                2024-01-02,100.00,105.00,99.00,103.00,103.00,1000000
                2024-01-03,103.00,107.00,102.00,106.00,106.00,1200000
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(2, data.size());
        assertEquals("2024-01-02", data.get(0).date());
        assertEquals(new BigDecimal("100.00"), data.get(0).open());
        assertEquals(new BigDecimal("105.00"), data.get(0).high());
        assertEquals(new BigDecimal("99.00"), data.get(0).low());
        assertEquals(new BigDecimal("103.00"), data.get(0).close());
        assertEquals(1000000L, data.get(0).volume());
    }

    @Test
    void loadFromStreamSkipsInvalidLines() {
        String csv = """
                Date,Open,High,Low,Close,Adj Close,Volume
                2024-01-02,100.00,105.00,99.00,103.00,103.00,1000000
                2024-01-03,null,null,null,null,null,0
                2024-01-04,110.00,115.00,109.00,113.00,113.00,900000
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(2, data.size());
        assertEquals("2024-01-02", data.get(0).date());
        assertEquals("2024-01-04", data.get(1).date());
    }

    @Test
    void loadFromStreamWithMissingAdjCloseUsesClose() {
        String csv = """
                Date,Open,High,Low,Close
                2024-01-02,100.00,105.00,99.00,103.00
                """;

        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);

        assertEquals(1, data.size());
        assertEquals(new BigDecimal("103.00"), data.get(0).adjClose());
        assertEquals(0L, data.get(0).volume());
    }

    @Test
    void loadFromFile() throws Exception {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
                Date,Open,High,Low,Close,Adj Close,Volume
                2024-01-02,50.00,55.00,49.00,53.00,53.00,500000
                """);

        List<PriceData> data = CsvPriceDataLoader.loadFromFile(csvFile.toString());

        assertEquals(1, data.size());
        assertEquals("2024-01-02", data.get(0).date());
        assertEquals(new BigDecimal("53.00"), data.get(0).close());
    }

    @Test
    void loadFromNonexistentFileReturnsEmpty() {
        List<PriceData> data = CsvPriceDataLoader.loadFromFile("/nonexistent/file.csv");
        assertTrue(data.isEmpty());
    }

    @Test
    void loadEmptyStream() {
        String csv = "Date,Open,High,Low,Close,Adj Close,Volume\n";
        var input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);
        assertTrue(data.isEmpty());
    }
}
