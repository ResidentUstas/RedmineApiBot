package ru.krista.fm.redmine.helpers;

import ru.krista.fm.redmine.exceptions.ExportServiceException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class DateHelper {
    private static String RIA_DATE_PATTERN = "dd.MM.yyyy h:mm:ss";
    private static String ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static Date asDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date asDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date asDate(String dateTimeString) throws ParseException {
        if (dateTimeString == null || dateTimeString.isEmpty()) return null;
        return new SimpleDateFormat(ISO_DATE_PATTERN).parse(dateTimeString);
    }

    public static Date asDateByPattern(String dateTimeString, String pattern) throws ParseException {
        if (dateTimeString == null || dateTimeString.isEmpty()) return null;
        return new SimpleDateFormat(pattern).parse(dateTimeString);
    }

    public static LocalDate asLocalDate(String dateTimeString) throws ParseException {
        return asLocalDate(dateTimeString, RIA_DATE_PATTERN);
    }

    public static LocalDate asLocalDate(String dateTimeString, String pattern) throws ParseException {
        return asLocalDate(asDateByPattern(dateTimeString, pattern));
    }

    public static Integer asUnvFromLocaleDateStr(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(RIA_DATE_PATTERN);
        LocalDate date = LocalDate.parse(value, formatter);
        return asUnv(date);
    }

    public static Integer asUnv(LocalDate date) {
        if (date == null) return null;
        var year = date.getYear();
        var month = date.getMonth().getValue();
        var day = date.getDayOfMonth();
        return (year * 10000) + (month * 100) + day;
    }

    public static LocalDate asLocalDate(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static String asLocalDateStr(LocalDate localDate) {
        if (localDate == null) return null;
        return localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static LocalDateTime asLocalDateTime(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date getDate(int year, int month, int day) {
        var startDate = Calendar.getInstance();
        startDate.set(year, month, day);
        return startDate.getTime();
    }

    public static LocalDate getLocalDate(int year, int month, int day) {
        var startDate = Calendar.getInstance();
        startDate.set(year, month, day);
        return asLocalDate(startDate.getTime());
    }

    public static String getDateStr(int periodId) throws ExportServiceException {
        var year = getYear(periodId);
        var month = getMonth(periodId);
        var day = getDayNotNull(periodId);
        var date = getDate(year, month - 1, day);
        return asLocalDateStr(date);
    }

    public static String asLocalDateStr(Date date) {
        if (date == null) return null;
        return asLocalDateStr(asLocalDate(date));
    }

    /**
     * Получение года периода.
     */
    public static int getYear(int periodId) throws ExportServiceException {
        checkPeriodId(periodId);
        return periodId / 10000;
    }

    /**
     * Получение года периода.
     */
    public static long getYear(long periodId) throws ExportServiceException {
        checkPeriodId(periodId);
        return periodId / 10000L;
    }

    /**
     * Получение месяца идентификатора.
     *
     * @param periodId Идентификатор отчетного периода.
     * @return Для периодов "Год", "Полугодие", "Квартал" возвращается месяц завершения периода.<br>
     * Для месяцев и дат возвращается соответствующий месяц.
     */
    public static int getMonth(int periodId) throws ExportServiceException {
        checkPeriodId(periodId);
        if (isYear(periodId))
            return 12; // Для периодов "Год", "Полугодие", "Квартал" возвращаем месяц завершения периода.
        if (isHalfYear(periodId)) return getQuarter(periodId) == 1 ? 6 : 12;
        if (isQuarter(periodId)) return getQuarter(periodId) * 3;
        return (periodId / 100) % 100; // Для месяцев и дат возвращаем соответствующий месяц.
    }

    /**
     * Проверка, является ли идентификатор периодом "Год".
     */
    public static boolean isYear(int periodId) {
        var date = periodId % 10000;
        return date == 1;
    }

    /**
     * Проверка, является ли идентификатор периодом "Полугодие".
     */
    public static boolean isHalfYear(int periodId) {
        var date = periodId % 10000;
        return date == 10 || date == 20;
    }

    /**
     * Проверка, является ли идентификатор периодом "Квартал".
     */
    public static boolean isQuarter(int periodId) {
        var date = periodId % 10000;
        return date >= 9991 && date <= 9994;
    }

    /**
     * Получение квартала периода.
     *
     * @return Для периодов "Год", "Полугодие" возвращается квартал завершения периода.<br>
     * Для кварталов, месяцев и дат возвращается соответствующий квартал.
     */
    public static int getQuarter(int periodId) throws ExportServiceException {
        if (isYear(periodId)) return 4;
        if (isHalfYear(periodId)) return getHalfYear(periodId) == 1 ? 2 : 4;
        if (isQuarter(periodId)) return periodId % 10;
        var month = (periodId / 100) % 100;
        return (month + 2) / 3;
    }

    /**
     * Получение полугодия периода.
     *
     * @return Для периодов "Год" возвращается 2 полугодие.<br>
     * Для остальных возвращается соответствующее полугодие.
     */
    public static int getHalfYear(int periodId) throws ExportServiceException {
        if (isYear(periodId)) return 2;
        if (isHalfYear(periodId)) return (periodId % 10000) / 10;
        if (isQuarter(periodId)) return periodId % 10 < 3 ? 1 : 2;
        return getMonth(periodId) < 7 ? 1 : 2;
    }

    /**
     * Проверка, является ли объект c идентификатором periodId периодом "Месяц".
     */
    public static boolean isMonth(int periodId) {
        var day = periodId % 100;
        var date = periodId % 10000;
        return day == 0 && date >= 100 && date <= 1200;
    }

    /**
     * Получение идентификатор периода, соответствующего месяцу.
     *
     * @return ID периода для записей месяцев в формате: YYYYMM00
     */
    public static int getMonthUnvId(int year, int month) throws ExportServiceException {
        checkMonth(month);
        return (year * 10000) + (month * 100);
    }

    public static String getCurrentMonthName(LocalDate date, int... plus) {
        int addVal = 0;
        if (plus.length > 0) addVal = plus[0];
        Month month = date.getMonth();
        int numMonth = month.getValue() == 12 ? addVal > 0 ? addVal : month.getValue() : month.getValue() + addVal;
        var result = getMonthName(numMonth, false);
        return result;
    }

    public static int getCurrentYear(LocalDate date) {
        Month month = date.getMonth();
        int year = month.getValue() == 11 ? date.getYear() + 1 : date.getYear();
        return year;
    }

    /**
     * Получения месяца строкой с добавлением нуля (0) если месяц < 10.
     */
    public static String getMonthStrAsNumber(int month) throws ExportServiceException {
        checkMonth(month);
        return month < 10 ? "0" + month : month + "";
    }

    /**
     * Получение идентификатор периода, соответствующего точной дате.
     *
     * @return ID периода для записей точных дат в формате: YYYYMMDD
     */
    public static int getDayUnvId(int year, int month, int day) throws ExportServiceException {
        checkMonth(month);
        if (day < 1 || day > YearMonth.of(year, month).lengthOfMonth()) {
            throw new ExportServiceException("Некорректное значение параметра day.");
        }
        return (year * 10000) + (month * 100) + day;
    }

    public static int getSizeOfMonth(int year, int month) throws ExportServiceException {
        checkMonth(month);
        return YearMonth.of(year, month).lengthOfMonth();
    }

    public static java.sql.Date addMonths(LocalDate currentDate, int countMounths) {
        Calendar calendar = new GregorianCalendar();
        calendar.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());
        calendar.add(Calendar.MONTH, countMounths);

        return new java.sql.Date(calendar.getTimeInMillis());
    }

    public static long getAmountDaysBetween(Date date1, Date date2) {
        long dateBefore = date1.getTime();
        long dateAfter = date2.getTime();

        long days = TimeUnit.MILLISECONDS.toDays(Math.abs(dateAfter - dateBefore));
        return days;
    }

    public static String toShortDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        return sdf.format(date);
    }

    public static Date addDays(LocalDate currentDate, int countDays) {
        Calendar calendar = new GregorianCalendar();
        calendar.set(currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());
        calendar.add(Calendar.DAY_OF_MONTH, countDays);
        return new java.sql.Date(calendar.getTimeInMillis());
    }

    /**
     * Возвращает UNV-дату начала месяца
     */
    public static int getMonthBeginUNV(int year, int month) {
        return (year * 10000) + (month * 100) + 1;
    }

    /**
     * Возвращает UNV-дату окончания месяца
     */
    public static int getMonthEndUNV(int year, int month) throws ExportServiceException {
        return getMonthEndUNV(year, month, false);
    }

    public static String getMonthName(int number, boolean isInDate) {
        Calendar calendar = new GregorianCalendar();
        String[] monthNames = {"январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"};
        String[] monthNamesInDate = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        String month = isInDate ? monthNamesInDate[number - 1] : monthNames[number - 1];
        return month;
    }

    /**
     * Возвращает UNV-дату окончания месяца
     */
    public static int getMonthEndUNV(int year, int month, boolean includeZO) throws ExportServiceException {
        var unv = (year * 10000) + (month * 100) + getSizeOfMonth(year, month);
        return (includeZO && month == 12) ? unv + 1 : unv;
    }

    private static int getDayNotNull(int periodId) throws ExportServiceException {
        if (isYear(periodId)) return 31;
        if (isHalfYear(periodId)) return getHalfYear(periodId) == 1 ? 30 : 31;
        if (isQuarter(periodId)) return ((getQuarter(periodId) == 1) || (getQuarter(periodId) == 4)) ? 31 : 30;
        if (isMonth(periodId)) {
            // Проверяем, что период - точная дата:
            var year = getYear(periodId);
            var month = getMonth(periodId);
            return YearMonth.of(year, month).lengthOfMonth();
        }
        return periodId % 100;
    }

    private static void checkMonth(int month) throws ExportServiceException {
        if (month < 1 || month > 12) throw new ExportServiceException("Некорректное значение параметра month.");
    }

    private static void checkPeriodId(int periodId) throws ExportServiceException {
        if (periodId < 0) throw new ExportServiceException("Отрицательные значения периодов недопустимы.");
    }

    private static void checkPeriodId(long periodId) throws ExportServiceException {
        if (periodId < 0) throw new ExportServiceException("Отрицательные значения периодов недопустимы.");
    }
}
