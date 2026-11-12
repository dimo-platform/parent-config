package com.dimo.parent.utils;

import lombok.experimental.UtilityClass;

import java.time.*;

@UtilityClass
public class DateUtil {


    /// parse from string

    public LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr);
    }

    public LocalDate parseLocalDate(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    public LocalDateTime parseDateTime(String dateStr) {
        return LocalDateTime.parse(dateStr);
    }

    public LocalDateTime parseDateTime(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }


    /// to string

    public String toString(LocalDate date) {
        return date.toString();
    }

    public String toString(LocalDateTime date) {
        return date.toString();
    }

}
