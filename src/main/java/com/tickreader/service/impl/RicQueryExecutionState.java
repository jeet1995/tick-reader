package com.tickreader.service.impl;

import com.tickreader.entity.Tick;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class RicQueryExecutionState {

    private final PriorityQueue<Tick> ticks;
    private final List<RicQueryExecutionStateByDate> ricQueryExecutionStatesByDate;
    private boolean isCompleted;
    private final int totalTickCount;

    /**
     * Constructs a new RicQueryExecutionState with the specified contexts organized by date.
     *
     * @param ricQueryExecutionStatesByDate List of RicQueryExecutionStateByDate objects
     * @param totalTickCount Maximum number of ticks to collect
     */
    public RicQueryExecutionState(List<RicQueryExecutionStateByDate> ricQueryExecutionStatesByDate, int totalTickCount) {
        this.ticks = new PriorityQueue<>((t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));
        this.ricQueryExecutionStatesByDate = ricQueryExecutionStatesByDate;
        this.isCompleted = false;
        this.totalTickCount = totalTickCount;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a single date group with the provided contexts.
     *
     * @param tickRequestContexts List of TickRequestContextPerPartitionKey objects
     * @param dateKey String representation of the date (e.g., "20241008")
     * @param totalTickCount Maximum number of ticks to collect
     */
    public RicQueryExecutionState(List<TickRequestContextPerPartitionKey> tickRequestContexts, String dateKey, int totalTickCount) {
        this.ticks = new PriorityQueue<>((t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));
        RicQueryExecutionStateByDate singleDateState = new RicQueryExecutionStateByDate(tickRequestContexts, dateKey);
        this.ricQueryExecutionStatesByDate = new ArrayList<>();
        this.ricQueryExecutionStatesByDate.add(singleDateState);
        this.isCompleted = false;
        this.totalTickCount = totalTickCount;
    }

    public synchronized void addTicks(List<Tick> ticksToAdd) {
        for (Tick tick : ticksToAdd) {
            // Only add if we haven't reached the total tick count
            if (this.ticks.size() < totalTickCount) {
                this.ticks.offer(tick);
            } else {
                // If priority queue is full, check if this tick has higher priority
                Tick lowestPriorityTick = this.ticks.peek();
                if (tick.getMessageTimestamp() > lowestPriorityTick.getMessageTimestamp()) {
                    this.ticks.poll(); // Remove lowest priority tick
                    this.ticks.offer(tick); // Add new tick
                }
            }
        }

        // Check if we've reached the target
        if (this.ticks.size() >= totalTickCount) {
            this.setCompleted(true);
        }
    }

    public synchronized List<Tick> getTicks() {
        List<Tick> result = new ArrayList<>();
        while (!this.ticks.isEmpty()) {
            result.add(this.ticks.poll());
        }
        return result;
    }

    public synchronized boolean isCompleted() {
        return this.isCompleted;
    }

    public synchronized void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    /**
     * Gets the current number of ticks in the priority queue.
     *
     * @return Current number of ticks
     */
    public synchronized int getCurrentTickCount() {
        return this.ticks.size();
    }

    /**
     * Gets the total target tick count.
     *
     * @return Total target number of ticks
     */
    public int getTotalTickCount() {
        return this.totalTickCount;
    }

    /**
     * Gets the list of RicQueryExecutionStateByDate objects.
     *
     * @return List of RicQueryExecutionStateByDate objects
     */
    public List<RicQueryExecutionStateByDate> getRicQueryExecutionStatesByDate() {
        return ricQueryExecutionStatesByDate;
    }

    /**
     * Gets all TickRequestContextPerPartitionKey objects across all date groups.
     * This method flattens the date-organized structure for backward compatibility.
     *
     * @return List of all TickRequestContextPerPartitionKey objects
     */
    public List<TickRequestContextPerPartitionKey> getTickRequestContexts() {
        return ricQueryExecutionStatesByDate.stream()
                .flatMap(dateState -> dateState.getTickRequestContexts().stream())
                .collect(Collectors.toList());
    }

    /**
     * Sets the target tick count for all date groups.
     *
     * @param targetTickCount The target number of ticks to collect per date group
     */
    public void setTargetTickCount(int targetTickCount) {
        ricQueryExecutionStatesByDate.forEach(dateState -> dateState.setTargetTickCount(targetTickCount));
    }

    /**
     * Gets the total current tick count across all date groups.
     *
     * @return Total number of ticks collected across all date groups
     */
    public int getTotalCurrentTickCount() {
        return ricQueryExecutionStatesByDate.stream()
                .mapToInt(RicQueryExecutionStateByDate::getCurrentTickCount)
                .sum();
    }

    /**
     * Gets the total target tick count across all date groups.
     *
     * @return Total target number of ticks across all date groups
     */
    public int getTotalTargetTickCount() {
        return ricQueryExecutionStatesByDate.stream()
                .mapToInt(RicQueryExecutionStateByDate::getTargetTickCount)
                .sum();
    }

    /**
     * Checks if the overall target tick count is reached.
     *
     * @return true if overall target is reached, false otherwise
     */
    public boolean isOverallTargetReached() {
        return getCurrentTickCount() >= totalTickCount;
    }

    /**
     * Gets the next active date group that hasn't reached its target.
     *
     * @return The next active RicQueryExecutionStateByDate, or null if all are completed
     */
    public RicQueryExecutionStateByDate getNextActiveDateGroup() {
        return ricQueryExecutionStatesByDate.stream()
                .filter(dateState -> !dateState.isCompleted() && !dateState.isTargetReached())
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all active date groups that haven't reached their targets.
     *
     * @return List of active RicQueryExecutionStateByDate objects
     */
    public List<RicQueryExecutionStateByDate> getActiveDateGroups() {
        return ricQueryExecutionStatesByDate.stream()
                .filter(dateState -> !dateState.isCompleted() && !dateState.isTargetReached())
                .collect(Collectors.toList());
    }

    /**
     * Executes sequential draining strategy across date groups.
     * Drains one date group at a time until the overall target is reached.
     *
     * @param executorService Executor service for parallel execution
     * @param fetchFunction Function to fetch next page for a context
     * @return CompletableFuture that completes when overall target is reached or all contexts are drained
     */
    public CompletableFuture<Void> drainSequentialStrategy(ExecutorService executorService,
                                                         java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction) {
        return CompletableFuture.runAsync(() -> {
            for (RicQueryExecutionStateByDate dateState : ricQueryExecutionStatesByDate) {
                // Skip if overall target is reached
                if (isOverallTargetReached()) {
                    break;
                }

                // Drain this date state completely
                try {
                    dateState.drainParallel(this, executorService, fetchFunction).get();
                } catch (Exception e) {
                    // Log error but continue with next date state
                    System.err.println("Error draining date state " + dateState.getDateKey() + ": " + e.getMessage());
                }

                // Check if overall target is reached after draining this date state
                if (isOverallTargetReached()) {
                    break;
                }
            }
        }, executorService);
    }

    /**
     * Legacy method for backward compatibility.
     * Executes parallel draining strategy across all date groups.
     * Drains each date group in parallel until target tick count is reached.
     *
     * @param executorService Executor service for parallel execution
     * @param fetchFunction Function to fetch next page for a context
     * @return CompletableFuture that completes when all targets are reached or all contexts are drained
     */
    public CompletableFuture<Void> drainParallelStrategy(ExecutorService executorService,
                                                       java.util.function.BiFunction<RicQueryExecutionState, TickRequestContextPerPartitionKey, CompletableFuture<Void>> fetchFunction) {
        List<CompletableFuture<Void>> dateGroupTasks = ricQueryExecutionStatesByDate.stream()
                .map(dateState -> dateState.drainParallel(this, executorService, fetchFunction))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(dateGroupTasks.toArray(new CompletableFuture[0]));
    }
}
