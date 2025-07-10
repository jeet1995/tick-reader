package com.tickreader.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Encapsulates a list of TickRequestContextPerPartitionKey objects for a specific date.
 * This class provides a clean abstraction for managing query execution contexts
 * organized by date, which is useful for multi-date queries and container management.
 */
public class RicQueryExecutionStateByDate {

    private final List<TickRequestContextPerPartitionKey> tickRequestContexts;
    private final String dateKey;
    private boolean isCompleted;
    private int targetTickCount;
    private int currentTickCount;
    private RicQueryExecutionState parentState;

    /**
     * Constructs a new RicQueryExecutionStateByDate with the specified contexts and date key.
     *
     * @param tickRequestContexts List of TickRequestContextPerPartitionKey objects
     * @param dateKey String representation of the date (e.g., "20241008")
     */
    public RicQueryExecutionStateByDate(List<TickRequestContextPerPartitionKey> tickRequestContexts, String dateKey) {
        this.tickRequestContexts = tickRequestContexts;
        this.dateKey = dateKey;
        this.isCompleted = false;
        this.targetTickCount = 0;
        this.currentTickCount = 0;
    }

    /**
     * Gets the list of TickRequestContextPerPartitionKey objects.
     *
     * @return List of TickRequestContextPerPartitionKey objects
     */
    public List<TickRequestContextPerPartitionKey> getTickRequestContexts() {
        return tickRequestContexts;
    }

    /**
     * Gets the date key associated with this execution state.
     *
     * @return String representation of the date
     */
    public String getDateKey() {
        return dateKey;
    }

    /**
     * Checks if this date group is completed.
     *
     * @return true if this date group is completed, false otherwise
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Sets the parent RicQueryExecutionState for this date group.
     *
     * @param parentState The parent RicQueryExecutionState
     */
    public void setParentState(RicQueryExecutionState parentState) {
        this.parentState = parentState;
    }

    /**
     * Sets the completion status for this date group.
     *
     * @param completed true if this date group is completed, false otherwise
     */
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        
        // Update parent state completion status if this date state is completed
        if (completed && parentState != null) {
            parentState.updateCompletionStatus();
        }
    }

    /**
     * Sets the target tick count for this date group.
     *
     * @param targetTickCount The target number of ticks to collect
     */
    public void setTargetTickCount(int targetTickCount) {
        this.targetTickCount = targetTickCount;
    }

    /**
     * Gets the target tick count for this date group.
     *
     * @return The target number of ticks to collect
     */
    public int getTargetTickCount() {
        return targetTickCount;
    }

    /**
     * Gets the current tick count for this date group.
     *
     * @return The current number of ticks collected
     */
    public int getCurrentTickCount() {
        return currentTickCount;
    }

    /**
     * Adds ticks to the current count and checks if target is reached.
     *
     * @param tickCount Number of ticks to add
     * @return true if target tick count is reached, false otherwise
     */
    public synchronized boolean addTicks(int tickCount) {
        this.currentTickCount += tickCount;
        if (this.currentTickCount >= this.targetTickCount) {
            this.isCompleted = true;
            
            // Update parent state completion status when target is reached
            if (parentState != null) {
                parentState.updateCompletionStatus();
            }
            
            return true;
        }
        return false;
    }

    /**
     * Checks if target tick count is reached.
     *
     * @return true if target tick count is reached, false otherwise
     */
    public boolean isTargetReached() {
        return currentTickCount >= targetTickCount;
    }

    /**
     * Gets the remaining tick count needed to reach target.
     *
     * @return Number of ticks still needed
     */
    public int getRemainingTickCount() {
        return Math.max(0, targetTickCount - currentTickCount);
    }

    /**
     * Checks if all contexts in this date group are drained (no more data to fetch).
     *
     * @return true if all contexts are drained, false otherwise
     */
    public boolean isAllContextsDrained() {
        return tickRequestContexts.stream()
                .allMatch(context -> "drained".equals(context.getContinuationToken()));
    }

    /**
     * Gets the number of contexts in this date group.
     *
     * @return Number of TickRequestContextPerPartitionKey objects
     */
    public int getContextCount() {
        return tickRequestContexts.size();
    }

    /**
     * Checks if this date group has any active (non-drained) contexts.
     *
     * @return true if there are active contexts, false if all are drained
     */
    public boolean hasActiveContexts() {
        return tickRequestContexts.stream()
                .anyMatch(context -> !"drained".equals(context.getContinuationToken()));
    }

    /**
     * Gets active contexts that are not drained.
     *
     * @return List of active TickRequestContextPerPartitionKey objects
     */
    public List<TickRequestContextPerPartitionKey> getActiveContexts() {
        return tickRequestContexts.stream()
                .filter(context -> !"drained".equals(context.getContinuationToken()))
                .collect(Collectors.toList());
    }

    /**
     * Executes parallel draining across all active contexts in this date group.
     *
     * @param executorService Executor service for parallel execution
     * @param fetchFunction Function to fetch next page for a context
     * @return CompletableFuture that completes when all contexts are drained or target is reached
     */
    public CompletableFuture<Void> drainParallel(RicQueryExecutionState ricQueryExecutionState, ExecutorService executorService,
                                               java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction) {
        List<TickRequestContextPerPartitionKey> activeContexts = getActiveContexts();
        
        if (activeContexts.isEmpty()) {
            this.isCompleted = true;
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> tasks = activeContexts.stream()
                .map(context -> CompletableFuture.runAsync(() -> {
                    try {
                        fetchFunction.apply(ricQueryExecutionState, context).get();
                    } catch (Exception e) {
                        // Log error but continue with other contexts
                        System.err.println("Error draining context: " + e.getMessage());
                    }
                }, executorService))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }
} 