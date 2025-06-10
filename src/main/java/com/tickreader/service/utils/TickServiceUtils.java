package com.tickreader.service.utils;

import com.azure.cosmos.CosmosException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class TickServiceUtils {

    private TickServiceUtils() {}

    public static List<String> getLocalDatesBetweenTwoLocalDateTimes(LocalDateTime startTime, LocalDateTime endTime, String format) {
        List<String> dates = new ArrayList<>();

        LocalDate current = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        while (!current.isAfter(endDate)) {
            dates.add(current.format(DateTimeFormatter.ofPattern(format)));
            current = current.plusDays(1);
        }

        return dates;
    }

    public static String constructTickIdentifierPrefix(String ric, String date) {
        return String.format("%s|%s", ric, date);
    }

    public static boolean isResourceNotFound(CosmosException e) {
        return e.getStatusCode() == 404 && e.getSubStatusCode() == 0;
    }
}