package com.stockdownloader.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Date utility providing market-aware date calculations and multiple format support.
 */
public class DateHelper {

    public static final DateTimeFormatter STANDARD_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter YAHOO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter YAHOO_EARNINGS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MORNINGSTAR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Calendar yesterdayMarket;
    private final Calendar todayMarket;
    private final Calendar tomorrowMarket;

    private final String today;
    private final String tomorrow;
    private final String yesterday;
    private final String sixMonthsAgo;

    private final String currentMonth;
    private final String currentDay;
    private final String currentYear;
    private final String fromMonth;
    private final String fromDay;
    private final String fromYear;

    private final DateFormat dateFormat;

    public DateHelper() throws ParseException {
        LocalDate now = LocalDate.now();
        LocalDate tomorrowDate = now.plusDays(1);
        LocalDate yesterdayDate = now.minusDays(1);
        LocalDate customDate = now.minusMonths(6);

        dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        yesterdayMarket = Calendar.getInstance();
        todayMarket = Calendar.getInstance();
        tomorrowMarket = Calendar.getInstance();

        yesterdayMarket.add(Calendar.DAY_OF_MONTH, adjustForWeekend(yesterdayDate.getDayOfWeek(), -1));
        adjustTodayMarket(now.getDayOfWeek());
        adjustTomorrowMarket(tomorrowDate.getDayOfWeek());

        yesterday = dateFormat.format(yesterdayMarket.getTime());
        today = dateFormat.format(todayMarket.getTime());
        tomorrow = dateFormat.format(tomorrowMarket.getTime());

        Calendar dateCustom = Calendar.getInstance();
        dateCustom.add(Calendar.MONTH, -6);
        sixMonthsAgo = dateFormat.format(dateCustom.getTime());

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");

        currentMonth = now.format(monthFmt);
        currentDay = now.format(dayFmt);
        currentYear = now.format(yearFmt);
        fromMonth = customDate.format(monthFmt);
        fromDay = customDate.format(dayFmt);
        fromYear = customDate.format(yearFmt);
    }

    public static LocalDate adjustToMarketDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.minusDays(1);
            case SUNDAY -> date.minusDays(2);
            default -> date;
        };
    }

    private int adjustForWeekend(DayOfWeek day, int defaultOffset) {
        return switch (day) {
            case SATURDAY -> -3;
            case SUNDAY -> -4;
            default -> defaultOffset;
        };
    }

    private void adjustTodayMarket(DayOfWeek day) {
        if (day == DayOfWeek.SATURDAY) todayMarket.add(Calendar.DAY_OF_MONTH, -1);
        if (day == DayOfWeek.SUNDAY) todayMarket.add(Calendar.DAY_OF_MONTH, -2);
    }

    private void adjustTomorrowMarket(DayOfWeek day) {
        switch (day) {
            case SATURDAY -> tomorrowMarket.add(Calendar.DAY_OF_MONTH, 3);
            case SUNDAY -> tomorrowMarket.add(Calendar.DAY_OF_MONTH, 2);
            default -> tomorrowMarket.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    public Calendar getYesterdayMarket() { return yesterdayMarket; }
    public Calendar getTodayMarket() { return todayMarket; }
    public Calendar getTomorrowMarket() { return tomorrowMarket; }
    public String getToday() { return today; }
    public String getTomorrow() { return tomorrow; }
    public String getYesterday() { return yesterday; }
    public String getSixMonthsAgo() { return sixMonthsAgo; }
    public String getCurrentMonth() { return currentMonth; }
    public String getCurrentDay() { return currentDay; }
    public String getCurrentYear() { return currentYear; }
    public String getFromMonth() { return fromMonth; }
    public String getFromDay() { return fromDay; }
    public String getFromYear() { return fromYear; }
    public DateFormat getDateFormat() { return dateFormat; }
}
