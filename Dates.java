import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Dates {
    // Actual Dates
    private Calendar dateToday;
    private Calendar dateTomorrow;
    private Calendar dateCustom;
    private Calendar dateYesterday;
    private Calendar fromDate;
    private Calendar toDate;

    // Actual Date Strings
    private String today;
    private String tomorrow;
    private String custom;
    private String yesterday;

    // Adjusted Dates
    private Calendar yesterdayMarket;
    private Calendar todayMarket;
    private Calendar tomorrowMarket;

    // Fiscal quarters
    private Calendar fiscalQtr1;
    private Calendar fiscalQtr2;
    private Calendar fiscalQtr3;
    private Calendar fiscalQtrPrevious;
    private Calendar fiscalQtrCurrent;

    // Fiscal quarters strings
    private String fiscalQtr1Str;
    private String fiscalQtr2Str;
    private String fiscalQtr3Str;
    private String fiscalQtrPreviousStr;
    private String fiscalQtrCurrentStr;

    // Date properties
    private String currentMonth;
    private String currentDay;
    private String currentYear;
    private String fromMonth;
    private String fromDay;
    private String fromYear;

    // Date formats
    private DateFormat monthFormat;
    private DateFormat dayFormat;
    private DateFormat yearFormat;
    private DateFormat dateFormat;
    private DateFormat zacksFormat;
    private DateFormat nasdaqFormat;
    private DateFormat yahooFormat;
    private DateFormat yahooEarningsFormat;
    private DateFormat morningstarFormat;
    private DateFormat timeFormat;

    public Dates() throws ParseException {

        // Dates
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

        // Formats
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

        // Change dates
        dateCustom.add(Calendar.MONTH, -6);
        dateTomorrow.add(Calendar.DAY_OF_MONTH, 1);
        dateYesterday.add(Calendar.DAY_OF_MONTH, -1);

        // Format dates
        currentMonth = monthFormat.format(dateToday.getTime());
        currentDay = dayFormat.format(dateToday.getTime());
        currentYear = yearFormat.format(dateToday.getTime());
        fromMonth = monthFormat.format(dateCustom.getTime());
        fromDay = dayFormat.format(dateCustom.getTime());
        fromYear = yearFormat.format(dateCustom.getTime());

        // Adjust if yesterday is Saturday
        if (dateYesterday.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            yesterdayMarket.add(Calendar.DAY_OF_MONTH, -3);

        // Adjust if yesterday is Sunday
        else if (dateYesterday.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            yesterdayMarket.add(Calendar.DAY_OF_MONTH, -4);

        // Default setting
        else
            yesterdayMarket.add(Calendar.DAY_OF_MONTH, -1);

        // Adjust if today is Saturday
        if (dateToday.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            todayMarket.add(Calendar.DAY_OF_MONTH, -1);

        // Adjust if today is Sunday
        if (dateToday.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            todayMarket.add(Calendar.DAY_OF_MONTH, -2);

        // Adjust if tomorrow is Saturday
        if (dateTomorrow.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            tomorrowMarket.add(Calendar.DAY_OF_MONTH, 3);

        // Adjust if tomorrow is Sunday
        else if (dateTomorrow.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            tomorrowMarket.add(Calendar.DAY_OF_MONTH, 2);

        // Default setting
        else
            tomorrowMarket.add(Calendar.DAY_OF_MONTH, 1);

        // Define Strings
        yesterday = dateFormat.format(yesterdayMarket.getTime());
        today = dateFormat.format(todayMarket.getTime());
        tomorrow = dateFormat.format(tomorrowMarket.getTime());
        custom = dateFormat.format(dateCustom.getTime());

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

    public Calendar getDateToday() {
        return dateToday;
    }

    public void setDateToday(Calendar dateToday) {
        this.dateToday = dateToday;
    }

    public Calendar getDateTomorrow() {
        return dateTomorrow;
    }

    public void setDateTomorrow(Calendar dateTomorrow) {
        this.dateTomorrow = dateTomorrow;
    }

    public Calendar getDateCustom() {
        return dateCustom;
    }

    public void setDateCustom(Calendar dateCustom) {
        this.dateCustom = dateCustom;
    }

    public Calendar getDateYesterday() {
        return dateYesterday;
    }

    public void setDateYesterday(Calendar dateYesterday) {
        this.dateYesterday = dateYesterday;
    }

    public Calendar getFromDate() {
        return fromDate;
    }

    public void setFromDate(Calendar fromDate) {
        this.fromDate = fromDate;
    }

    public Calendar getToDate() {
        return toDate;
    }

    public void setToDate(Calendar toDate) {
        this.toDate = toDate;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public String getTomorrow() {
        return tomorrow;
    }

    public void setTomorrow(String tomorrow) {
        this.tomorrow = tomorrow;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public String getYesterday() {
        return yesterday;
    }

    public void setYesterday(String yesterday) {
        this.yesterday = yesterday;
    }

    public Calendar getYesterdayMarket() {
        return yesterdayMarket;
    }

    public void setYesterdayMarket(Calendar yesterdayMarket) {
        this.yesterdayMarket = yesterdayMarket;
    }

    public Calendar getTodayMarket() {
        return todayMarket;
    }

    public void setTodayMarket(Calendar todayMarket) {
        this.todayMarket = todayMarket;
    }

    public Calendar getTomorrowMarket() {
        return tomorrowMarket;
    }

    public void setTomorrowMarket(Calendar tomorrowMarket) {
        this.tomorrowMarket = tomorrowMarket;
    }

    public Calendar getFiscalQtr1() {
        return fiscalQtr1;
    }

    public void setFiscalQtr1(Calendar fiscalQtr1) {
        this.fiscalQtr1 = fiscalQtr1;
    }

    public Calendar getFiscalQtr2() {
        return fiscalQtr2;
    }

    public void setFiscalQtr2(Calendar fiscalQtr2) {
        this.fiscalQtr2 = fiscalQtr2;
    }

    public Calendar getFiscalQtr3() {
        return fiscalQtr3;
    }

    public void setFiscalQtr3(Calendar fiscalQtr3) {
        this.fiscalQtr3 = fiscalQtr3;
    }

    public Calendar getFiscalQtrPrevious() {
        return fiscalQtrPrevious;
    }

    public void setFiscalQtrPrevious(Calendar fiscalQtrPrevious) {
        this.fiscalQtrPrevious = fiscalQtrPrevious;
    }

    public Calendar getFiscalQtrCurrent() {
        return fiscalQtrCurrent;
    }

    public void setFiscalQtrCurrent(Calendar fiscalQtrCurrent) {
        this.fiscalQtrCurrent = fiscalQtrCurrent;
    }

    public String getFiscalQtr1Str() {
        return fiscalQtr1Str;
    }

    public void setFiscalQtr1Str(String fiscalQtr1Str) {
        this.fiscalQtr1Str = fiscalQtr1Str;
    }

    public String getFiscalQtr2Str() {
        return fiscalQtr2Str;
    }

    public void setFiscalQtr2Str(String fiscalQtr2Str) {
        this.fiscalQtr2Str = fiscalQtr2Str;
    }

    public String getFiscalQtr3Str() {
        return fiscalQtr3Str;
    }

    public void setFiscalQtr3Str(String fiscalQtr3Str) {
        this.fiscalQtr3Str = fiscalQtr3Str;
    }

    public String getFiscalQtrPreviousStr() {
        return fiscalQtrPreviousStr;
    }

    public void setFiscalQtrPreviousStr(String fiscalQtrPreviousStr) {
        this.fiscalQtrPreviousStr = fiscalQtrPreviousStr;
    }

    public String getFiscalQtrCurrentStr() {
        return fiscalQtrCurrentStr;
    }

    public void setFiscalQtrCurrentStr(String fiscalQtrCurrentStr) {
        this.fiscalQtrCurrentStr = fiscalQtrCurrentStr;
    }

    public String getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(String currentMonth) {
        this.currentMonth = currentMonth;
    }

    public String getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(String currentDay) {
        this.currentDay = currentDay;
    }

    public String getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(String currentYear) {
        this.currentYear = currentYear;
    }

    public String getFromMonth() {
        return fromMonth;
    }

    public void setFromMonth(String fromMonth) {
        this.fromMonth = fromMonth;
    }

    public String getFromDay() {
        return fromDay;
    }

    public void setFromDay(String fromDay) {
        this.fromDay = fromDay;
    }

    public String getFromYear() {
        return fromYear;
    }

    public void setFromYear(String fromYear) {
        this.fromYear = fromYear;
    }

    public DateFormat getMonthFormat() {
        return monthFormat;
    }

    public void setMonthFormat(DateFormat monthFormat) {
        this.monthFormat = monthFormat;
    }

    public DateFormat getDayFormat() {
        return dayFormat;
    }

    public void setDayFormat(DateFormat dayFormat) {
        this.dayFormat = dayFormat;
    }

    public DateFormat getYearFormat() {
        return yearFormat;
    }

    public void setYearFormat(DateFormat yearFormat) {
        this.yearFormat = yearFormat;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DateFormat getZacksFormat() {
        return zacksFormat;
    }

    public void setZacksFormat(DateFormat zacksFormat) {
        this.zacksFormat = zacksFormat;
    }

    public DateFormat getNasdaqFormat() {
        return nasdaqFormat;
    }

    public void setNasdaqFormat(DateFormat nasdaqFormat) {
        this.nasdaqFormat = nasdaqFormat;
    }

    public DateFormat getYahooFormat() {
        return yahooFormat;
    }

    public void setYahooFormat(DateFormat yahooFormat) {
        this.yahooFormat = yahooFormat;
    }

    public DateFormat getYahooEarningsFormat() {
        return yahooEarningsFormat;
    }

    public void setYahooEarningsFormat(DateFormat yahooEarningsFormat) {
        this.yahooEarningsFormat = yahooEarningsFormat;
    }

    public DateFormat getMorningstarFormat() {
        return morningstarFormat;
    }

    public void setMorningstarFormat(DateFormat morningstarFormat) {
        this.morningstarFormat = morningstarFormat;
    }

    public DateFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(DateFormat timeFormat) {
        this.timeFormat = timeFormat;
    }
}