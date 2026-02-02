package com.stockdownloader.analysis;

import com.stockdownloader.model.AlertResult;
import com.stockdownloader.model.PriceData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SignalGenerator.
 */
class SignalGeneratorTest {

    @Test
    void generateAlert_withSufficientData() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        assertNotNull(alert);
        assertEquals("TEST", alert.symbol());
        assertNotNull(alert.direction());
        assertNotNull(alert.callRecommendation());
        assertNotNull(alert.putRecommendation());
        assertTrue(alert.currentPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void generateAlert_withInsufficientData() {
        List<PriceData> data = generateTestData(50);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        assertNotNull(alert);
        assertEquals(AlertResult.Direction.NEUTRAL, alert.direction());
    }

    @Test
    void generateAlert_hasIndicators() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        assertNotNull(alert.bullishIndicators());
        assertNotNull(alert.bearishIndicators());
        // Should have at least some indicators detected
        assertTrue(alert.bullishIndicators().size() + alert.bearishIndicators().size() > 0,
                "Should detect at least one indicator");
    }

    @Test
    void generateAlert_hasSupportResistance() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        assertNotNull(alert.supportLevels());
        assertNotNull(alert.resistanceLevels());
    }

    @Test
    void generateAlert_optionsRecommendation() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        assertNotNull(alert.callRecommendation().type());
        assertNotNull(alert.putRecommendation().type());
        assertNotNull(alert.callRecommendation().action());
        assertNotNull(alert.putRecommendation().action());
    }

    @Test
    void generateAlert_toString_doesNotThrow() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        String output = alert.toString();
        assertNotNull(output);
        assertTrue(output.contains("TRADING ALERT"));
        assertTrue(output.contains("TEST"));
    }

    @Test
    void generateAlert_signalStrength() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data);

        String strength = alert.getSignalStrength();
        assertNotNull(strength);
        assertTrue(strength.contains("%"));
    }

    @Test
    void generateAlert_atSpecificIndex() {
        List<PriceData> data = generateTestData(300);
        AlertResult alert = SignalGenerator.generateAlert("TEST", data, 250);

        assertNotNull(alert);
        assertEquals("TEST", alert.symbol());
    }

    private static List<PriceData> generateTestData(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100.0;
        for (int i = 0; i < days; i++) {
            price += (Math.random() - 0.48) * 3;
            price = Math.max(50, price);
            double high = price + Math.random() * 3;
            double low = price - Math.random() * 3;
            data.add(new PriceData("2020-01-01", BigDecimal.valueOf(price - 1),
                    BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                    BigDecimal.valueOf(price), BigDecimal.valueOf(price),
                    (long) (1_000_000 + Math.random() * 5_000_000)));
        }
        return data;
    }
}
