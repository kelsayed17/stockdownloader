package com.stockdownloader.util;

import com.stockdownloader.model.OptionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BlackScholesCalculatorTest {

    private static final BigDecimal SPOT = new BigDecimal("100");
    private static final BigDecimal STRIKE_ATM = new BigDecimal("100");
    private static final BigDecimal STRIKE_OTM_CALL = new BigDecimal("110");
    private static final BigDecimal STRIKE_ITM_CALL = new BigDecimal("90");
    private static final BigDecimal TIME_30D = new BigDecimal("0.0822"); // ~30/365
    private static final BigDecimal TIME_1Y = BigDecimal.ONE;
    private static final BigDecimal RATE = new BigDecimal("0.05");
    private static final BigDecimal VOL = new BigDecimal("0.20");

    @Test
    void callPriceIsPositive() {
        BigDecimal price = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(price.doubleValue() > 0, "ATM call should have positive price");
    }

    @Test
    void putPriceIsPositive() {
        BigDecimal price = BlackScholesCalculator.price(
                OptionType.PUT, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(price.doubleValue() > 0, "ATM put should have positive price");
    }

    @Test
    void callPriceIncreasesWithLongerExpiry() {
        BigDecimal short30d = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        BigDecimal long1y = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_1Y, RATE, VOL);
        assertTrue(long1y.compareTo(short30d) > 0,
                "Longer expiry should increase call price");
    }

    @Test
    void otmCallCheaperThanAtmCall() {
        BigDecimal atm = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        BigDecimal otm = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_OTM_CALL, TIME_30D, RATE, VOL);
        assertTrue(atm.compareTo(otm) > 0, "OTM call should be cheaper than ATM");
    }

    @Test
    void itmCallMoreExpensiveThanAtmCall() {
        BigDecimal atm = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        BigDecimal itm = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ITM_CALL, TIME_30D, RATE, VOL);
        assertTrue(itm.compareTo(atm) > 0, "ITM call should be more expensive than ATM");
    }

    @Test
    void putCallParity() {
        // Put-call parity: C - P = S - K*exp(-rT)
        BigDecimal callPrice = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_1Y, RATE, VOL);
        BigDecimal putPrice = BlackScholesCalculator.price(
                OptionType.PUT, SPOT, STRIKE_ATM, TIME_1Y, RATE, VOL);

        double S = SPOT.doubleValue();
        double K = STRIKE_ATM.doubleValue();
        double r = RATE.doubleValue();
        double T = TIME_1Y.doubleValue();

        double lhs = callPrice.doubleValue() - putPrice.doubleValue();
        double rhs = S - K * Math.exp(-r * T);

        assertEquals(rhs, lhs, 0.01, "Put-call parity should hold");
    }

    @Test
    void expiredOptionReturnsIntrinsicValue() {
        BigDecimal itmCall = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ITM_CALL, BigDecimal.ZERO, RATE, VOL);
        // Intrinsic: 100 - 90 = 10
        assertEquals(0, new BigDecimal("10.0000").compareTo(itmCall));

        BigDecimal otmCall = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_OTM_CALL, BigDecimal.ZERO, RATE, VOL);
        assertEquals(0, BigDecimal.ZERO.compareTo(otmCall.stripTrailingZeros()));
    }

    @Test
    void zeroVolReturnsIntrinsic() {
        BigDecimal price = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ITM_CALL, TIME_30D, RATE, BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("10.0000").compareTo(price));
    }

    @Test
    void callDeltaBetweenZeroAndOne() {
        BigDecimal d = BlackScholesCalculator.delta(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(d.doubleValue() > 0 && d.doubleValue() < 1,
                "Call delta should be between 0 and 1, got " + d);
    }

    @Test
    void putDeltaBetweenNegOneAndZero() {
        BigDecimal d = BlackScholesCalculator.delta(
                OptionType.PUT, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(d.doubleValue() > -1 && d.doubleValue() < 0,
                "Put delta should be between -1 and 0, got " + d);
    }

    @Test
    void atmCallDeltaAroundPointFive() {
        BigDecimal d = BlackScholesCalculator.delta(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(d.doubleValue() > 0.4 && d.doubleValue() < 0.7,
                "ATM call delta should be around 0.5, got " + d);
    }

    @Test
    void thetaIsNegativeForLongOption() {
        BigDecimal t = BlackScholesCalculator.theta(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, VOL);
        assertTrue(t.doubleValue() < 0, "Theta should be negative for long call");
    }

    @Test
    void estimateVolatilityFromPrices() {
        BigDecimal[] prices = new BigDecimal[21];
        // Simulate slight uptrend with some noise
        for (int i = 0; i < 21; i++) {
            prices[i] = new BigDecimal(100 + i * 0.5 + Math.sin(i) * 2);
        }

        BigDecimal vol = BlackScholesCalculator.estimateVolatility(prices, 20);
        assertTrue(vol.doubleValue() > 0, "Volatility should be positive");
        assertTrue(vol.doubleValue() < 2.0, "Volatility should be reasonable");
    }

    @Test
    void estimateVolatilityDefaultsForInsufficientData() {
        BigDecimal vol = BlackScholesCalculator.estimateVolatility(null, 20);
        assertEquals(0, new BigDecimal("0.20").compareTo(vol.setScale(2)));
    }

    @Test
    void intrinsicValueCalculation() {
        assertEquals(0, new BigDecimal("10.0000").compareTo(
                BlackScholesCalculator.intrinsicValue(OptionType.CALL, SPOT, STRIKE_ITM_CALL)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                BlackScholesCalculator.intrinsicValue(OptionType.CALL, SPOT, STRIKE_OTM_CALL).stripTrailingZeros()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(
                BlackScholesCalculator.intrinsicValue(OptionType.PUT, SPOT, STRIKE_OTM_CALL)));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                BlackScholesCalculator.intrinsicValue(OptionType.PUT, SPOT, STRIKE_ITM_CALL).stripTrailingZeros()));
    }

    @Test
    void higherVolIncreasesPrice() {
        BigDecimal lowVol = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, new BigDecimal("0.10"));
        BigDecimal highVol = BlackScholesCalculator.price(
                OptionType.CALL, SPOT, STRIKE_ATM, TIME_30D, RATE, new BigDecimal("0.40"));
        assertTrue(highVol.compareTo(lowVol) > 0, "Higher vol should increase option price");
    }
}
