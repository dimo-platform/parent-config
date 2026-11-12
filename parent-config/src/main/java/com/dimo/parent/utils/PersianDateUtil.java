package com.dimo.parent.utils;

import com.github.mfathi91.time.PersianDate;
import com.github.mfathi91.time.PersianDateTime;
import lombok.experimental.UtilityClass;

import java.time.*;

@UtilityClass
public class PersianDateUtil {


    ///  parse Persian dates and date times

    public PersianDate parseDate(String dateStr) {
        return PersianDate.parse(dateStr);
    }

    public PersianDate parseDate(int year, int month, int day) {
        return PersianDate.of(year, month, day);
    }

    public PersianDateTime parseDateTime(String dateStr) {
        return PersianDateTime.parse(dateStr);
    }

    public PersianDateTime parseDateTime(int year, int month, int day, int hour, int minute, int second) {
        return PersianDateTime.of(year, month, day, hour, minute, second);
    }


    /// to Persian date

    public PersianDate toPersianDate(LocalDate date) {
        return PersianDate.from(date);
    }


    /// to Persian date time

    public PersianDateTime toPersianDateTime(LocalDateTime dateTime) {
        return PersianDateTime.from(dateTime);
    }


    /// current

    public PersianDate now() {
        return PersianDate.now();
    }

    public PersianDateTime nowDateTime() {
        return PersianDateTime.now();
    }


    /// to Gregorian

    public LocalDate toGregorianDate(PersianDate date) {
        return date.toGregorian();
    }

    public LocalDateTime toGregorianDateTime(PersianDateTime date) {
        return date.toGregorian();
    }
}