package com.stockdownloader.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Date utility providing market-aware date calculations and multiple format support.
 * Uses java.time exclusively for all date operations.
 */
public final class DateHelper {

    public static final DateTimeFormatter STANDARD_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter YAHOO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter YAHOO_EARNINGS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MORNINGSTAR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final LocalDate referenceDate;
    private final LocalDate yesterdayMarket;
    private final LocalDate todayMarket;
    private final LocalDate tomorrowMarket;
    private final LocalDate sixMonthsAgoDate;

    public DateHelper() {
        this(LocalDate.now());
    }

    public DateHelper(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
        this.todayMarket = adjustToMarketDay(referenceDate);
        this.yesterdayMarket = adjustToMarketDay(referenceDate.minusDays(1));
        this.tomorrowMarket = adjustToNextMarketDay(referenceDate.plusDays(1));
        this.sixMonthsAgoDate = referenceDate.minusMonths(6);
    }

    public static LocalDate adjustToMarketDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.minusDays(1);
            case SUNDAY -> date.minusDays(2);
            default -> date;
        };
    }

    private static LocalDate adjustToNextMarketDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.plusDays(2);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }

    public LocalDate getYesterdayMarket() { return yesterdayMarket; }
    public LocalDate getTodayMarket() { return todayMarket; }
    public LocalDate getTomorrowMarket() { return tomorrowMarket; }

    public String getToday() { return todayMarket.format(STANDARD_FORMAT); }
    public String getTomorrow() { return tomorrowMarket.format(STANDARD_FORMAT); }
    public String getYesterday() { return yesterdayMarket.format(STANDARD_FORMAT); }
    public String getSixMonthsAgo() { return sixMonthsAgoDate.format(STANDARD_FORMAT); }

    public String getCurrentMonth() { return referenceDate.format(MONTH_FMT); }
    public String getCurrentDay() { return referenceDate.format(DAY_FMT); }
    public String getCurrentYear() { return referenceDate.format(YEAR_FMT); }
    public String getFromMonth() { return sixMonthsAgoDate.format(MONTH_FMT); }
    public String getFromDay() { return sixMonthsAgoDate.format(DAY_FMT); }
    public String getFromYear() { return sixMonthsAgoDate.format(YEAR_FMT); }
}
