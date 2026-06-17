package com.payhub.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class DateUtil {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYYMMDD = "yyyyMMdd";

    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD);

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

    public static final DateTimeFormatter DATE_COMPACT_FORMATTER = DateTimeFormatter.ofPattern(YYYYMMDD);

    public static final DateTimeFormatter DATETIME_COMPACT_FORMATTER = DateTimeFormatter.ofPattern(YYYYMMDDHHMMSS);

    private DateUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }

    public static String formatCompactDate(LocalDate date) {
        return date == null ? null : date.format(DATE_COMPACT_FORMATTER);
    }

    public static String formatCompactDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_COMPACT_FORMATTER);
    }

    public static LocalDate parseDate(String dateStr) {
        return dateStr == null || dateStr.isEmpty() ? null : LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return dateTimeStr == null || dateTimeStr.isEmpty() ? null : LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    public static LocalDate parseCompactDate(String dateStr) {
        return dateStr == null || dateStr.isEmpty() ? null : LocalDate.parse(dateStr, DATE_COMPACT_FORMATTER);
    }

    public static LocalDateTime parseCompactDateTime(String dateTimeStr) {
        return dateTimeStr == null || dateTimeStr.isEmpty() ? null : LocalDateTime.parse(dateTimeStr, DATETIME_COMPACT_FORMATTER);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDate localDate) {
        return localDate == null ? null : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(start, end);
    }

    public static LocalDate firstDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate lastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    public static long toTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? 0L : dateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    public static boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && expireTime.isBefore(now());
    }
}
