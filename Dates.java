import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Dates {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter ZACKS_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter NASDAQ_FORMAT = DateTimeFormatter.ofPattern("yyyy-MMM-dd");
    private static final DateTimeFormatter YAHOO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter YAHOO_EARNINGS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MORNINGSTAR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    // Legacy Calendar/DateFormat support for backward compatibility
    private final Calendar dateToday;
    private final Calendar dateTomorrow;
    private final Calendar dateCustom;
    private final Calendar dateYesterday;
    private final Calendar fromDate;
    private final Calendar toDate;
    private final Calendar yesterdayMarket;
    private final Calendar todayMarket;
    private final Calendar tomorrowMarket;
    private Calendar fiscalQtr1;
    private Calendar fiscalQtr2;
    private Calendar fiscalQtr3;
    private Calendar fiscalQtrPrevious;
    private Calendar fiscalQtrCurrent;

    private final String today;
    private final String tomorrow;
    private final String custom;
    private final String yesterday;

    private String fiscalQtr1Str;
    private String fiscalQtr2Str;
    private String fiscalQtr3Str;
    private String fiscalQtrPreviousStr;
    private String fiscalQtrCurrentStr;

    private final String currentMonth;
    private final String currentDay;
    private final String currentYear;
    private final String fromMonth;
    private final String fromDay;
    private final String fromYear;

    private final DateFormat dateFormat;
    private final DateFormat zacksFormat;
    private final DateFormat nasdaqFormat;
    private final DateFormat yahooFormat;
    private final DateFormat yahooEarningsFormat;
    private final DateFormat morningstarFormat;
    private final DateFormat monthFormat;
    private final DateFormat dayFormat;
    private final DateFormat yearFormat;
    private final DateFormat timeFormat;

    public Dates() throws ParseException {
        LocalDate now = LocalDate.now();
        LocalDate tomorrowDate = now.plusDays(1);
        LocalDate yesterdayDate = now.minusDays(1);
        LocalDate customDate = now.minusMonths(6);

        // Initialize date formats
        monthFormat = new SimpleDateFormat("MM");
        dayFormat = new SimpleDateFormat("dd");
        yearFormat = new SimpleDateFormat("yyyy");
        dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        zacksFormat = new SimpleDateFormat("MM-dd-yyyy");
        nasdaqFormat = new SimpleDateFormat("yyyy-MMM-dd");
        yahooFormat = new SimpleDateFormat("yyyy-MM-dd");
        yahooEarningsFormat = new SimpleDateFormat("yyyyMMdd");
        morningstarFormat = new SimpleDateFormat("yyyy-MM");
        timeFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

        // Calendar instances
        dateToday = Calendar.getInstance();
        dateTomorrow = Calendar.getInstance();
        dateCustom = Calendar.getInstance();
        dateYesterday = Calendar.getInstance();
        fromDate = Calendar.getInstance();
        toDate = Calendar.getInstance();
        yesterdayMarket = Calendar.getInstance();
        todayMarket = Calendar.getInstance();
        tomorrowMarket = Calendar.getInstance();
        fiscalQtr1 = Calendar.getInstance();
        fiscalQtr2 = Calendar.getInstance();
        fiscalQtr3 = Calendar.getInstance();
        fiscalQtrPrevious = Calendar.getInstance();
        fiscalQtrCurrent = Calendar.getInstance();

        dateTomorrow.add(Calendar.DAY_OF_MONTH, 1);
        dateYesterday.add(Calendar.DAY_OF_MONTH, -1);
        dateCustom.add(Calendar.MONTH, -6);

        // Format date components
        currentMonth = now.format(MONTH_FORMAT);
        currentDay = now.format(DAY_FORMAT);
        currentYear = now.format(YEAR_FORMAT);
        fromMonth = customDate.format(MONTH_FORMAT);
        fromDay = customDate.format(DAY_FORMAT);
        fromYear = customDate.format(YEAR_FORMAT);

        // Adjust market dates for weekends
        yesterdayMarket.add(Calendar.DAY_OF_MONTH, adjustForWeekend(yesterdayDate.getDayOfWeek(), -1));
        adjustTodayMarket(now.getDayOfWeek());
        adjustTomorrowMarket(tomorrowDate.getDayOfWeek());

        yesterday = dateFormat.format(yesterdayMarket.getTime());
        today = dateFormat.format(todayMarket.getTime());
        tomorrow = dateFormat.format(tomorrowMarket.getTime());
        custom = dateFormat.format(dateCustom.getTime());

        // Fiscal quarter dates
        fiscalQtr1Str = "03/28/2016";
        fiscalQtr2Str = "06/28/2016";
        fiscalQtr3Str = "09/28/2016";
        fiscalQtrPreviousStr = "03/28/2016";
        fiscalQtrCurrentStr = "09/28/2016";

        fiscalQtr1.setTime(dateFormat.parse(fiscalQtr1Str));
        fiscalQtr2.setTime(dateFormat.parse(fiscalQtr2Str));
        fiscalQtr3.setTime(dateFormat.parse(fiscalQtr3Str));
        fiscalQtrPrevious.setTime(dateFormat.parse(fiscalQtrPreviousStr));
        fiscalQtrCurrent.setTime(dateFormat.parse(fiscalQtrCurrentStr));

        fromDate.setTime(dateFormat.parse(custom));
        toDate.setTime(dateFormat.parse(today));
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

    // Getters
    public Calendar getDateToday() { return dateToday; }
    public Calendar getDateTomorrow() { return dateTomorrow; }
    public Calendar getDateCustom() { return dateCustom; }
    public Calendar getDateYesterday() { return dateYesterday; }
    public Calendar getFromDate() { return fromDate; }
    public Calendar getToDate() { return toDate; }
    public String getToday() { return today; }
    public String getTomorrow() { return tomorrow; }
    public String getCustom() { return custom; }
    public String getYesterday() { return yesterday; }
    public Calendar getYesterdayMarket() { return yesterdayMarket; }
    public Calendar getTodayMarket() { return todayMarket; }
    public Calendar getTomorrowMarket() { return tomorrowMarket; }
    public Calendar getFiscalQtr1() { return fiscalQtr1; }
    public Calendar getFiscalQtr2() { return fiscalQtr2; }
    public Calendar getFiscalQtr3() { return fiscalQtr3; }
    public Calendar getFiscalQtrPrevious() { return fiscalQtrPrevious; }
    public Calendar getFiscalQtrCurrent() { return fiscalQtrCurrent; }
    public String getFiscalQtr1Str() { return fiscalQtr1Str; }
    public String getFiscalQtr2Str() { return fiscalQtr2Str; }
    public String getFiscalQtr3Str() { return fiscalQtr3Str; }
    public String getFiscalQtrPreviousStr() { return fiscalQtrPreviousStr; }
    public String getFiscalQtrCurrentStr() { return fiscalQtrCurrentStr; }
    public String getCurrentMonth() { return currentMonth; }
    public String getCurrentDay() { return currentDay; }
    public String getCurrentYear() { return currentYear; }
    public String getFromMonth() { return fromMonth; }
    public String getFromDay() { return fromDay; }
    public String getFromYear() { return fromYear; }
    public DateFormat getMonthFormat() { return monthFormat; }
    public DateFormat getDayFormat() { return dayFormat; }
    public DateFormat getYearFormat() { return yearFormat; }
    public DateFormat getDateFormat() { return dateFormat; }
    public DateFormat getZacksFormat() { return zacksFormat; }
    public DateFormat getNasdaqFormat() { return nasdaqFormat; }
    public DateFormat getYahooFormat() { return yahooFormat; }
    public DateFormat getYahooEarningsFormat() { return yahooEarningsFormat; }
    public DateFormat getMorningstarFormat() { return morningstarFormat; }
    public DateFormat getTimeFormat() { return timeFormat; }

    // Setters for mutable state
    public void setFiscalQtr1(Calendar v) { this.fiscalQtr1 = v; }
    public void setFiscalQtr2(Calendar v) { this.fiscalQtr2 = v; }
    public void setFiscalQtr3(Calendar v) { this.fiscalQtr3 = v; }
    public void setFiscalQtrPrevious(Calendar v) { this.fiscalQtrPrevious = v; }
    public void setFiscalQtrCurrent(Calendar v) { this.fiscalQtrCurrent = v; }
    public void setFiscalQtr1Str(String v) { this.fiscalQtr1Str = v; }
    public void setFiscalQtr2Str(String v) { this.fiscalQtr2Str = v; }
    public void setFiscalQtr3Str(String v) { this.fiscalQtr3Str = v; }
    public void setFiscalQtrPreviousStr(String v) { this.fiscalQtrPreviousStr = v; }
    public void setFiscalQtrCurrentStr(String v) { this.fiscalQtrCurrentStr = v; }
}
