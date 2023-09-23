package com.example.demo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.modelmapper.AbstractConverter;
import org.modelmapper.AbstractProvider;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;

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

    @Test
    public void testModelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        java.util.Date utilDate = modelMapper.map(System.currentTimeMillis(), java.util.Date.class);
        java.sql.Date sqlDate = modelMapper.map("2018-10-25", java.sql.Date.class);
        System.out.println("utilDate:" + utilDate);
        System.out.println("sqlDate:" + sqlDate);
        
        Provider<java.util.Date> javaDateProvider = new AbstractProvider<java.util.Date>() {
            @Override
            public java.util.Date get() {
                return new java.util.Date();
            }
        };

        Converter<String, java.util.Date> toStringDate = new AbstractConverter<String, java.util.Date>() {
            @Override
            protected java.util.Date convert(String source) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    java.util.Date date = sdf.parse(source);
                    return date;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };


        modelMapper.createTypeMap(String.class, java.util.Date.class);
        modelMapper.addConverter(toStringDate);
        modelMapper.getTypeMap(String.class, java.util.Date.class).setProvider(javaDateProvider);

        String dateTest = "2018-10-25";
        java.util.Date dateConverted = modelMapper.map(dateTest, java.util.Date.class);
        System.out.println("dateConverted:" + dateConverted);
        assertNotNull(dateConverted);
        assertEquals(dateTest, new SimpleDateFormat("yyyy-MM-dd").format(dateConverted));
 
        
        Provider<LocalDate> localDateProvider = new AbstractProvider<LocalDate>() {
            @Override
            public LocalDate get() {
                return LocalDate.now();
            }
        };

        Converter<String, LocalDate> toStringLocalDate = new AbstractConverter<String, LocalDate>() {
            @Override
            protected LocalDate convert(String source) {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate localDate = LocalDate.parse(source, format);
                return localDate;
            }
        };

        modelMapper.createTypeMap(String.class, LocalDate.class);
        modelMapper.addConverter(toStringLocalDate);
        modelMapper.getTypeMap(String.class, LocalDate.class).setProvider(localDateProvider);
        LocalDate localDateConverted = modelMapper.map(dateTest, LocalDate.class);
        System.out.println("localDateConverted:" + localDateConverted.toString());
        assertNotNull(localDateConverted);
        assertEquals(dateTest, localDateConverted.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

}