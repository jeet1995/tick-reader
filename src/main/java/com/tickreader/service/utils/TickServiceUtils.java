package com.tickreader.service.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TickServiceUtils {

    private TickServiceUtils() {}

    public static List<String> getLocalDatesBetweenTwoLocalDateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        List<String> dates = new ArrayList<>();

        LocalDate current = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        while (!current.isAfter(endDate)) {
            dates.add(current.toString()); // ISO format: yyyy-MM-dd
            current = current.plusDays(1);
        }

        return dates;
    }

    public static String constructTickIdentifier(String ric, String date, int shard) {
        return String.format("%s|%s|%d", ric, date, shard);
    }
}