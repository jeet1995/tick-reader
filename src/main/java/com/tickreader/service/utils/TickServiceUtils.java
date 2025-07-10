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

    public static TickRequestContextPerPartitionKey evaluateTickRequestContextToExecute(RicQueryExecutionState ricQueryExecutionState) {
        List<TickRequestContextPerPartitionKey> tickRequestContexts = ricQueryExecutionState.getTickRequestContexts();

        if (ricQueryExecutionState.isCompleted()) {
            return null;
        }

        if (tickRequestContexts.isEmpty()) {
            throw new IllegalStateException("No TickRequestContextPerPartitionKey found for the given RicQueryExecutionState.");
        }

        for (TickRequestContextPerPartitionKey tickRequestContextPerPartitionKey : tickRequestContexts) {

            if (tickRequestContextPerPartitionKey.getContinuationToken() == null || !tickRequestContextPerPartitionKey.getContinuationToken().equalsIgnoreCase("drained")) {
                return tickRequestContextPerPartitionKey;
            }
        }

        return null;
    }

    /**
     * Evaluates the next active date group for parallel draining strategy.
     *
     * @param ricQueryExecutionState The execution state to evaluate
     * @return The next active date group, or null if all are completed
     */
    public static RicQueryExecutionStateByDate evaluateNextActiveDateGroup(RicQueryExecutionState ricQueryExecutionState) {
        if (ricQueryExecutionState.isCompleted()) {
            return null;
        }

        return ricQueryExecutionState.getNextActiveDateGroup();
    }

    /**
     * Executes parallel draining strategy with tick count management.
     * Drains all active contexts in parallel until target tick count is reached.
     *
     * @param ricQueryExecutionState The execution state to drain
     * @param executorService Executor service for parallel execution
     * @param fetchFunction Function to fetch next page for a context
     * @param targetTickCount Target number of ticks to collect
     * @return CompletableFuture that completes when target is reached or all contexts are drained
     */
    public static CompletableFuture<Void> executeParallelDrainingStrategy(
            RicQueryExecutionState ricQueryExecutionState,
            ExecutorService executorService,
            BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            int targetTickCount) {

        // Set target tick count for all date groups
        ricQueryExecutionState.setTargetTickCount(targetTickCount);

        // Execute parallel draining strategy
        return ricQueryExecutionState.drainParallelStrategy(executorService, fetchFunction);
    }

    /**
     * Executes parallel draining strategy with basic fetchNextPage functionality.
     * This method integrates the existing fetchNextPage methods with the parallel draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the specific fetchNextPage method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeParallelDrainingWithFetchNextPage(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainParallelStrategy(executorService, fetchFunction);
    }

    /**
     * Executes parallel draining strategy with range filters functionality.
     * This method integrates the existing fetchNextPageWithRangeFilters methods with the parallel draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithRangeFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeParallelDrainingWithRangeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainParallelStrategy(executorService, fetchFunction);
    }

    /**
     * Executes parallel draining strategy with price-volume filters functionality.
     * This method integrates the existing fetchNextPageWithPriceVolumeFilters methods with the parallel draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithPriceVolumeFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeParallelDrainingWithPriceVolumeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainParallelStrategy(executorService, fetchFunction);
    }

    /**
     * Executes parallel draining strategy with qualifiers filters functionality.
     * This method integrates the existing fetchNextPageWithQualifiersFilters methods with the parallel draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithQualifiersFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeParallelDrainingWithQualifiersFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainParallelStrategy(executorService, fetchFunction);
    }

    /**
     * Executes sequential draining strategy with basic fetchNextPage functionality.
     * This method integrates the existing fetchNextPage methods with the sequential draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the specific fetchNextPage method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeSequentialDrainingWithFetchNextPage(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainSequentialStrategy(executorService, fetchFunction);
    }

    /**
     * Executes sequential draining strategy with range filters functionality.
     * This method integrates the existing fetchNextPageWithRangeFilters methods with the sequential draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithRangeFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeSequentialDrainingWithRangeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainSequentialStrategy(executorService, fetchFunction);
    }

    /**
     * Executes sequential draining strategy with price-volume filters functionality.
     * This method integrates the existing fetchNextPageWithPriceVolumeFilters methods with the sequential draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithPriceVolumeFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeSequentialDrainingWithPriceVolumeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainSequentialStrategy(executorService, fetchFunction);
    }

    /**
     * Executes sequential draining strategy with qualifiers filters functionality.
     * This method integrates the existing fetchNextPageWithQualifiersFilters methods with the sequential draining strategy.
     *
     * @param ricQueryExecutionState The execution state containing all contexts
     * @param fetchFunction The function wrapper for the fetchNextPageWithQualifiersFilters method
     * @param executorService The executor service for parallel execution
     * @return CompletableFuture that completes when all contexts are drained
     */
    public static CompletableFuture<Void> executeSequentialDrainingWithQualifiersFilters(
            RicQueryExecutionState ricQueryExecutionState,
            java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction,
            ExecutorService executorService) {
        
        return ricQueryExecutionState.drainSequentialStrategy(executorService, fetchFunction);
    }

    /**
     * Checks if the execution state has reached its target tick count.
     *
     * @param ricQueryExecutionState The execution state to check
     * @return true if target is reached, false otherwise
     */
    public static boolean isTargetReached(RicQueryExecutionState ricQueryExecutionState) {
        return ricQueryExecutionState.isOverallTargetReached();
    }

    /**
     * Gets the progress information for the execution state.
     *
     * @param ricQueryExecutionState The execution state to get progress for
     * @return String representation of progress (current/target ticks)
     */
    public static String getProgressInfo(RicQueryExecutionState ricQueryExecutionState) {
        int current = ricQueryExecutionState.getTotalCurrentTickCount();
        int target = ricQueryExecutionState.getTotalTargetTickCount();
        return String.format("Progress: %d/%d ticks (%.1f%%)", current, target, 
                target > 0 ? (double) current / target * 100 : 0.0);
    }

    public static String constructTickIdentifierPrefix(String ric, String date) {
        return String.format("%s|%s", ric, date);
    }

    public static boolean isResourceNotFound(CosmosException e) {
        return e.getStatusCode() == 404 && e.getSubStatusCode() == 0;
    }
}