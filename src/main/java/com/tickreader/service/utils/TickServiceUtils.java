package com.tickreader.service.utils;

import com.azure.cosmos.CosmosException;
import com.tickreader.service.impl.RicQueryExecutionState;
import com.tickreader.service.impl.RicQueryExecutionStateByDate;
import com.tickreader.service.impl.TickRequestContextPerPartitionKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class TickServiceUtils {

    private TickServiceUtils() {}

    public static List<String> getLocalDatesBetweenTwoLocalDateTimes(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String format,
            boolean pinStart) {

        List<String> dates = new ArrayList<>();

        if (pinStart) {

            LocalDate current = startTime.toLocalDate();
            LocalDate endDate = endTime.toLocalDate();

            while (!current.isAfter(endDate)) {
                dates.add(current.format(DateTimeFormatter.ofPattern(format)));
                current = current.plusDays(1);
            }
        } else {


            LocalDate current = endTime.toLocalDate();
            LocalDate endDate = startTime.toLocalDate();

            while (!current.isBefore(endDate)) {
                dates.add(current.format(DateTimeFormatter.ofPattern(format)));
                current = current.minusDays(1);
            }
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