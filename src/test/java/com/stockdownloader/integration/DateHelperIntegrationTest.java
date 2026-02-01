package com.stockdownloader.integration;

import com.stockdownloader.util.DateHelper;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the refactored DateHelper.
 * Verifies market-day adjustments and date formatting across various day-of-week scenarios.
 */
class DateHelperIntegrationTest {

    @Test
    void weekdayReferenceDate() {
        // Wednesday 2024-01-10
        var helper = new DateHelper(LocalDate.of(2024, 1, 10));

        assertEquals(LocalDate.of(2024, 1, 10), helper.getTodayMarket());
        assertEquals(LocalDate.of(2024, 1, 9), helper.getYesterdayMarket());
        assertEquals(LocalDate.of(2024, 1, 11), helper.getTomorrowMarket());

        assertEquals("01/10/2024", helper.getToday());
        assertEquals("01/09/2024", helper.getYesterday());
        assertEquals("01/11/2024", helper.getTomorrow());
    }

    @Test
    void saturdayReferenceDate() {
        // Saturday 2024-01-13
        var helper = new DateHelper(LocalDate.of(2024, 1, 13));

        // Today should snap back to Friday
        assertEquals(LocalDate.of(2024, 1, 12), helper.getTodayMarket());
        // Yesterday (Friday) should be Friday itself
        assertEquals(LocalDate.of(2024, 1, 12), helper.getYesterdayMarket());
        // Tomorrow (Sunday) -> next Monday
        assertEquals(LocalDate.of(2024, 1, 15), helper.getTomorrowMarket());
    }

    @Test
    void sundayReferenceDate() {
        // Sunday 2024-01-14
        var helper = new DateHelper(LocalDate.of(2024, 1, 14));

        // Today should snap back to Friday
        assertEquals(LocalDate.of(2024, 1, 12), helper.getTodayMarket());
        // Yesterday (Saturday) -> snaps back to Friday
        assertEquals(LocalDate.of(2024, 1, 12), helper.getYesterdayMarket());
        // Tomorrow (Monday) -> Monday
        assertEquals(LocalDate.of(2024, 1, 15), helper.getTomorrowMarket());
    }

    @Test
    void mondayReferenceDate() {
        // Monday 2024-01-15
        var helper = new DateHelper(LocalDate.of(2024, 1, 15));

        assertEquals(LocalDate.of(2024, 1, 15), helper.getTodayMarket());
        // Yesterday (Sunday) -> snaps back to Friday
        assertEquals(LocalDate.of(2024, 1, 12), helper.getYesterdayMarket());
        assertEquals(LocalDate.of(2024, 1, 16), helper.getTomorrowMarket());
    }

    @Test
    void fridayReferenceDate() {
        // Friday 2024-01-12
        var helper = new DateHelper(LocalDate.of(2024, 1, 12));

        assertEquals(LocalDate.of(2024, 1, 12), helper.getTodayMarket());
        assertEquals(LocalDate.of(2024, 1, 11), helper.getYesterdayMarket());
        // Tomorrow (Saturday) -> next Monday
        assertEquals(LocalDate.of(2024, 1, 15), helper.getTomorrowMarket());
    }

    @Test
    void sixMonthsAgoCalculation() {
        var helper = new DateHelper(LocalDate.of(2024, 7, 15));

        assertEquals("01/15/2024", helper.getSixMonthsAgo());
        assertEquals("01", helper.getFromMonth());
        assertEquals("15", helper.getFromDay());
        assertEquals("2024", helper.getFromYear());
    }

    @Test
    void currentDateComponents() {
        var helper = new DateHelper(LocalDate.of(2024, 3, 5));

        assertEquals("03", helper.getCurrentMonth());
        assertEquals("05", helper.getCurrentDay());
        assertEquals("2024", helper.getCurrentYear());
    }

    @Test
    void adjustToMarketDayStatic() {
        // Weekdays stay the same
        assertEquals(LocalDate.of(2024, 1, 10),
                DateHelper.adjustToMarketDay(LocalDate.of(2024, 1, 10)));
        assertEquals(LocalDate.of(2024, 1, 12),
                DateHelper.adjustToMarketDay(LocalDate.of(2024, 1, 12)));

        // Saturday -> Friday
        assertEquals(LocalDate.of(2024, 1, 12),
                DateHelper.adjustToMarketDay(LocalDate.of(2024, 1, 13)));

        // Sunday -> Friday
        assertEquals(LocalDate.of(2024, 1, 12),
                DateHelper.adjustToMarketDay(LocalDate.of(2024, 1, 14)));
    }

    @Test
    void allMarketDaysAreWeekdays() {
        // Test across a full week
        for (int day = 8; day <= 14; day++) {
            var helper = new DateHelper(LocalDate.of(2024, 1, day));

            DayOfWeek todayDow = helper.getTodayMarket().getDayOfWeek();
            assertNotEquals(DayOfWeek.SATURDAY, todayDow);
            assertNotEquals(DayOfWeek.SUNDAY, todayDow);

            DayOfWeek yesterdayDow = helper.getYesterdayMarket().getDayOfWeek();
            assertNotEquals(DayOfWeek.SATURDAY, yesterdayDow);
            assertNotEquals(DayOfWeek.SUNDAY, yesterdayDow);

            DayOfWeek tomorrowDow = helper.getTomorrowMarket().getDayOfWeek();
            assertNotEquals(DayOfWeek.SATURDAY, tomorrowDow);
            assertNotEquals(DayOfWeek.SUNDAY, tomorrowDow);
        }
    }

    @Test
    void defaultConstructorDoesNotThrow() {
        // The refactored DateHelper constructor no longer throws ParseException
        assertDoesNotThrow(DateHelper::new);
    }

    @Test
    void dateFormattingConsistency() {
        var helper = new DateHelper(LocalDate.of(2024, 12, 25));

        String today = helper.getToday();
        // Should match MM/dd/yyyy format
        assertTrue(today.matches("\\d{2}/\\d{2}/\\d{4}"),
                "Date should match MM/dd/yyyy format: " + today);
        assertEquals("12/25/2024", today);
    }
}
