package ru.krista.fm.redmine.helpers;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ReportHelper {

    /**
     * Возвращает список, инициализированный Null-ми
     */
    public static List<Object> nullList(int count) {
        return Arrays.stream(new Object[count]).toList();
    }

    /**
     * Возвращает сумму элементов в коллекции.
     * Если все элементы равны Null, то возвращает Null
     */
    public static Double nullSum(ArrayList<Double> source) {
        return source.stream().anyMatch(Objects::nonNull)
                ? source.stream().filter(Objects::nonNull).mapToDouble(x -> x).sum()
                : null;
    }

    /**
     * Возвращает сумму элементов в коллекции.
     * Если все элементы равны Null, то возвращает Null
     */
    public static Double nullSum(Object... values) {
        return Arrays.stream(values).anyMatch(Objects::nonNull)
                ? Arrays.stream(values).filter(Objects::nonNull).mapToDouble(x -> (Double) x).sum()
                : null;
    }

    /**
     * Возвращает значение типа Double или Null
     */
    public static Double getDouble(Object value) {
        try {
            return Double.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Double sumDouble(ArrayList<Double> source) {
        return source.stream().mapToDouble(x -> x).sum();
    }

    public static Long sumLong(ArrayList<Long> source) {
        return source.stream().mapToLong(x -> x).sum();
    }

    public static Integer sumInteger(ArrayList<Integer> source) {
        return source.stream().mapToInt(x -> x).sum();
    }

    /**
     * Возвращает сумму параметров
     * Если все элементы равны Null, то возвращает 0
     */
    public static Double zeroSum(Object... values) {
        return Arrays.stream(values).anyMatch(Objects::nonNull)
                ? Arrays.stream(values).filter(Objects::nonNull).mapToDouble(x -> (Double) x).sum()
                : 0D;
    }

    /**
     * Возвращает сумму элементов в коллекции.
     * Если все элементы равны Null, то возвращает Null
     */
    public static Double zeroSum(List<Double> source) {
        return source.stream().anyMatch(Objects::nonNull)
                ? source.stream().filter(Objects::nonNull).mapToDouble(x -> x).sum()
                : 0D;
    }

    public static String getPrecisionFormat(Integer precision) {
        return getPrecisionFormat(precision, 2);
    }

    public static String getPrecisionFormat(Integer precision, int defaultValue) {
        var val = precision != null ? precision : defaultValue;
        return switch (val) {
            case 0 -> "#,##0";
            case 1 -> "#,##0.0";
            case 2 -> "#,##0.00";
            default -> "#,##0.000";
        };
    }

    public static String getPrecisionPercentFormat(Integer precision) {
        return getPrecisionPercentFormat(precision, 2);
    }

    public static String getPrecisionPercentFormat(Integer precision, int defaultValue) {
        var val = precision != null ? precision : defaultValue;
        return switch (val) {
            case 0 -> "#,##0%";
            case 1 -> "#,##0.0%";
            case 2 -> "#,##0.00%";
            default -> "#,##0.000%";
        };
    }

    public static List<Integer> getRange(int start, int limit) {
        return IntStream.iterate(start, i -> i + 1)
                .limit(limit)
                .boxed()
                .toList();
    }

    public static List<Integer> getIntListFromParam(String valueStr) {
        if (valueStr.isBlank() || valueStr.isEmpty()) return new ArrayList<>();
        return Stream.of(valueStr.split(","))
                .mapToInt(Integer::parseInt)
                .boxed()
                .toList();
    }

    public static ArrayList<Integer> getIntArrayListFromParam(String valueStr) {
        if (valueStr.isBlank() || valueStr.isEmpty()) return new ArrayList<>();
        return Stream.of(valueStr.split(","))
                .mapToInt(Integer::parseInt)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Long> getLongListFromParam(String valueStr) {
        if (valueStr.isBlank() || valueStr.isEmpty()) return new ArrayList<>();
        return Stream.of(valueStr.split(","))
                .mapToLong(Long::parseLong)
                .boxed()
                .toList();
    }

    public static boolean arrayContainsIgnoreCase(String searchStr, List<String> list) {
        return list.stream().anyMatch(x -> x.equalsIgnoreCase(searchStr));
    }

    public static Pair<Integer, String> getDivider(Integer measure) {
        return getDivider(measure, 0);
    }

    public static Pair<Integer, String> getDivider(Integer measure, int defaultValue) {
        measure = measure == null ? defaultValue : measure;
        return switch (measure) {
            case 0 -> new Pair<>(1, getDividerText(1));
            case 1 -> new Pair<>(1000, getDividerText(1000));
            case 2 -> new Pair<>(1000000, getDividerText(1000000));
            case 3 -> new Pair<>(1000000000, getDividerText(1000000000));
            default -> new Pair<>(1, "Неизвестные единицы");
        };
    }

    public static String getDividerText(Integer divider) {
        return getDividerText(divider, 0);
    }

    public static String getDividerText(Integer divider, Integer defaultValue) {
        divider = divider == null ? defaultValue : divider;
        return switch (divider) {
            case 1 -> "руб.";
            case 1000 -> "тыс. руб.";
            case 1000000 -> "млн. руб.";
            case 1000000000 -> "млрд. руб.";
            default -> "1/{%s} руб.".formatted(divider);
        };
    }

    /**
     * Возвращает результат деления в процентах
     * @param value Делимое
     * @param divider Делитель
     */
    public static Double divide(Object value, Object divider) {
        return divide(value, divider, true);
    }

    /**
     * Возвращает результат деления
     * @param value Делимое
     * @param divider Делитель
     * @param inPerc Выразить в процентах
     */
    public static Double divide(Object value, Object divider, boolean inPerc) {
        var div = getDouble(divider);
        if (value != null && div != null && div != 0) {
            var doubleValue = getDouble(value);
            if (doubleValue == null) return null;
            var res = doubleValue / div;
            return inPerc ? res * 100 : res;
        }
        return null;
    }

    /**
     * Возвращает результат деления
     * @param value Делимое
     * @param divider Делитель
     * @param inPerc Выразить в процентах
     */
    public static Long longDivide(Object value, Object divider, boolean inPerc) {
        var div = Long.parseLong(divider.toString());
        if (value != null && div != 0) {
            var longValue = Long.parseLong((String) value);
            var res = longValue / div;
            return inPerc ? res * 100 : res;
        }
        return null;
    }

    /**
     * Возвращает результат деления
     */
    public static Double divide(double value, Double divider) {
        return divider != null && divider != 0 ? value / divider : null;
    }

    /**
     * Возвращает результат деления
     */
    public static Double divide(Double value, Double divider) {
        return value != null && divider != null && divider != 0 ? value / divider : null;
    }

    public static String getPeriodStr(Integer year, Integer month) {
        var monthStr = switch (month) {
            case 1 -> "Январь";
            case 2 -> "Февраль";
            case 3 -> "Март";
            case 4 -> "Апрель";
            case 5 -> "Май";
            case 6 -> "Июнь";
            case 7 -> "Июль";
            case 8 -> "Август";
            case 9 -> "Сентябрь";
            case 10 -> "Октябрь";
            case 11 -> "Ноябрь";
            case 12 -> "Декабрь";
            default -> "";
        };
        return "%s %s года".formatted(monthStr, year);
    }
}
