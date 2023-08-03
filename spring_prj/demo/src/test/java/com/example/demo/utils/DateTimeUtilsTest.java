package com.example.demo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;

public class DateTimeUtilsTest {
    @Test
    void testFormatLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        String expected = "2022/01/01 00:00:00";
        String actual = DateTimeUtils.formatDateTime(localDateTime);
        assertEquals(expected, actual);
    }

    @Test
    void testFormatY4MMDD_KANJI() {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        String expected = "2022年01月01日";
        String actual = DateTimeUtils.formatY4MMDD_KANJI(localDateTime);
        assertEquals(expected, actual);
    }

    @Test
    void testFormatY4MMDDE_KANJI() {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        String expected = "2022年01月01日(土)";
        String actual = DateTimeUtils.formatY4MMDDE_KANJI(localDateTime);
        assertEquals(expected, actual);
    }

    @Test
    void testParseDateTime() {
        String dateTimeString = "2022/08/01 12:34:56";
        LocalDateTime expected = LocalDateTime.of(2022, 8, 1, 12, 34, 56);
        LocalDateTime actual = DateTimeUtils.parseDateTime(dateTimeString);
        assertEquals(expected, actual);
    }

    @Test
    void testParseY4MMDD_KANJI() {
        String dateTimeString = "2022年08月01日";
        LocalDateTime expected = LocalDateTime.of(2022, 8, 1, 0, 0, 0);
        LocalDateTime actual = DateTimeUtils.parseY4MMDD_KANJI(dateTimeString);
        assertEquals(expected, actual);
    }

    @Test
    public void testConstructorReference() {
        // 初期化する要素を持つリストを作成
        List<String> initialList = Arrays.asList("apple", "banana", "orange");

        // コンストラクタ参照を使ってArrayListを初期化
        Function<List<String>, ArrayList<String>> constructor = ArrayList::new;
        List<String> list = constructor.apply(initialList);

        // 結果を表示
        System.out.println(list);

        // supplier.get()を呼ぶ度に新しいインスタンスが生成される
        Supplier<List<String>> supplier = ArrayList::new;
        List<String> list2 = supplier.get();
        list2.add("apple");
        List<String> list3 = supplier.get();
        list3.add("banana");
        assertNotEquals(list2, list3);
    }
}