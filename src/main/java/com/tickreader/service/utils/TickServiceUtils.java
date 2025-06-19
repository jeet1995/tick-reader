package com.tickreader.service.utils;

import com.azure.cosmos.CosmosException;
import com.tickreader.service.impl.TimeChunk;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    public static boolean isEpochChunkPartOfDay(TimeChunk timeChunk, LocalDate localDate) {
        long epochStart = timeChunk.getChunkEpochStart();
        long epochEnd = timeChunk.getChunkEpochEnd();

        LocalDate dayStartEpoch = nanoEpochToInstant(epochStart).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate dayEndEpoch = nanoEpochToInstant(epochEnd).atZone(ZoneOffset.UTC).toLocalDate();

        // Check if the time chunk overlaps with the given local date
        return localDate.equals(dayStartEpoch) && localDate.equals(dayEndEpoch);
    }

    public static List<TimeChunk> generateTimeChunks(Long startEpoch, Long endEpoch, int chunkCount) {
        List<TimeChunk> timeChunks = new ArrayList<>();

        long chunkSize = (endEpoch - startEpoch) / chunkCount;

        long currentStart = startEpoch;
        while (currentStart < endEpoch) {
            long currentEnd = Math.min(currentStart + chunkSize - 1, endEpoch);
            timeChunks.add(new TimeChunk(currentStart, currentEnd));
            currentStart = currentEnd;
        }

        return timeChunks;
    }

    private static Instant nanoEpochToInstant(long nanosSinceEpoch) {
        long seconds = nanosSinceEpoch / 1_000_000_000;
        int nanos = (int) (nanosSinceEpoch % 1_000_000_000);

        return Instant.ofEpochSecond(seconds, nanos);
    }

    public static String generateDocTypeQuerySubstring(List<String> docTypes) {

        if (docTypes == null || docTypes.size() == 0) {
            return "";
        }

        if (docTypes.contains("TAS")) {
            return " and ARRAY_CONTAINS([\"TAS\"], C.docType, false) " ;
        }

        if (docTypes.contains("TAS") && docTypes.contains("TAQ")) {
            return " and ARRAY_CONTAINS([\"TAS\", \"TAQ\"], C.docType, false) " ;
        }

        if (docTypes.contains("TAQ")) {
            return " and ARRAY_CONTAINS([\"TAQ\"], C.docType, false) " ;
        }

        return "";
    }
}