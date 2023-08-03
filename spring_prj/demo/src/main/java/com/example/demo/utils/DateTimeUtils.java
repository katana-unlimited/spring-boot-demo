package com.example.demo.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 日付・時刻のフォーマットを行うユーティリティクラス
 */
public class DateTimeUtils {
    private static DateTimeFormatter DATE_TIME     = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static DateTimeFormatter Y4MMDD_KANJI  = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static DateTimeFormatter Y4MMDDE_KANJI = DateTimeFormatter.ofPattern("yyyy年MM月dd日(E)", Locale.JAPANESE);

    /**
     * localDateTimeをyyyy/MM/dd HH:mm:ss形式の文字列に変換する
n     * @param localDateTime
     * @return yyyy/MM/dd HH:mm:ss形式の文字列
     */
    static public String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME);
    }

    /**
     * localDateTimeをyyyy年MM月dd日形式の文字列に変換する
     * @param localDateTime
     * @return yyyy年MM月dd日形式の文字列
     */
    static public String formatY4MMDD_KANJI(LocalDateTime localDateTime) {
        return localDateTime.format(Y4MMDD_KANJI);
    }

    /**
     * localDateTimeをyyyy年MM月dd日(E)形式の文字列に変換する
     * @param localDateTime
     * @return yyyy年MM月dd日(E)形式の文字列
     */
    static public String formatY4MMDDE_KANJI(LocalDateTime localDateTime) {
        return localDateTime.format(Y4MMDDE_KANJI);
    }

    /**
     * yyyy/MM/dd HH:mm:ss形式の文字列をLocalDateTimeに変換する
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, DATE_TIME);
    }

    /**
     * yyyy年MM月dd日形式の文字列をLocalDateTimeに変換する
     */
    public static LocalDateTime parseY4MMDD_KANJI(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        LocalDate date = LocalDate.parse(dateTimeString, Y4MMDD_KANJI);
        return date.atStartOfDay();
    }
}
