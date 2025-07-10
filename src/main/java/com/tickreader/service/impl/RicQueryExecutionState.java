package com.tickreader.service.impl;

import com.tickreader.entity.Tick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class RicQueryExecutionState {

    private final List<Tick> ticks;
    private final List<RicQueryExecutionStateByDate> ricQueryExecutionStatesByDate;
    private boolean isCompleted;

    /**
     * Constructs a new RicQueryExecutionState with the specified contexts organized by date.
     *
     * @param ricQueryExecutionStatesByDate List of RicQueryExecutionStateByDate objects
     */
    public RicQueryExecutionState(List<RicQueryExecutionStateByDate> ricQueryExecutionStatesByDate) {
        this.ticks = new ArrayList<>();
        this.ricQueryExecutionStatesByDate = ricQueryExecutionStatesByDate;
        this.isCompleted = false;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a single date group with the provided contexts.
     *
     * @param tickRequestContexts List of TickRequestContextPerPartitionKey objects
     * @param dateKey String representation of the date (e.g., "20241008")
     */
    public RicQueryExecutionState(List<TickRequestContextPerPartitionKey> tickRequestContexts, String dateKey) {
        this.ticks = new ArrayList<>();
        RicQueryExecutionStateByDate singleDateState = new RicQueryExecutionStateByDate(tickRequestContexts, dateKey);
        this.ricQueryExecutionStatesByDate = new ArrayList<>();
        this.ricQueryExecutionStatesByDate.add(singleDateState);
        this.isCompleted = false;
    }

    public synchronized void addTicks(List<Tick> ticksToAdd, int tickCount) {
        int size = this.ticks.size();
        int remainingTicks = tickCount - size;

        if (ticksToAdd.size() > remainingTicks) {
            ticksToAdd = ticksToAdd.subList(0, remainingTicks);
            this.ticks.addAll(ticksToAdd);
        } else {
            this.ticks.addAll(ticksToAdd);
        }

        if (this.ticks.size() == tickCount) {
            this.setCompleted(true);
        }
    }

    public synchronized List<Tick> getTicks() {
        return this.ticks;
    }

    public synchronized boolean isCompleted() {
        return this.isCompleted;
    }

    public synchronized void setCompleted(boolean completed) {
        this.isCompleted = completed;
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
     * Gets the list of RicQueryExecutionStateByDate objects.
     *
     * @return List of RicQueryExecutionStateByDate objects
     */
    public List<RicQueryExecutionStateByDate> getRicQueryExecutionStatesByDate() {
        return ricQueryExecutionStatesByDate;
    }

    /**
     * Checks if all contexts across all date groups are drained.
     *
     * @return true if all contexts are drained, false otherwise
     */
    public boolean isAllContextsDrained() {
        return ricQueryExecutionStatesByDate.stream()
                .allMatch(RicQueryExecutionStateByDate::isAllContextsDrained);
    }

    /**
     * Gets the total number of contexts across all date groups.
     *
     * @return Total number of TickRequestContextPerPartitionKey objects
     */
    public int getTotalContextCount() {
        return ricQueryExecutionStatesByDate.stream()
                .mapToInt(RicQueryExecutionStateByDate::getContextCount)
                .sum();
    }

    /**
     * Checks if there are any active contexts across all date groups.
     *
     * @return true if there are active contexts, false if all are drained
     */
    public boolean hasActiveContexts() {
        return ricQueryExecutionStatesByDate.stream()
                .anyMatch(RicQueryExecutionStateByDate::hasActiveContexts);
    }

    /**
     * Sets the completion status for a specific date group.
     *
     * @param dateKey The date key to set completion status for
     * @param completed true if the date group is completed, false otherwise
     */
    public void setDateGroupCompleted(String dateKey, boolean completed) {
        ricQueryExecutionStatesByDate.stream()
                .filter(dateState -> dateKey.equals(dateState.getDateKey()))
                .findFirst()
                .ifPresent(dateState -> dateState.setCompleted(completed));
    }

    /**
     * Checks if all date groups are completed.
     *
     * @return true if all date groups are completed, false otherwise
     */
    public boolean areAllDateGroupsCompleted() {
        return ricQueryExecutionStatesByDate.stream()
                .allMatch(RicQueryExecutionStateByDate::isCompleted);
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
        return getTotalCurrentTickCount() >= getTotalTargetTickCount();
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
                .map(dateState -> dateState.drainParallel(executorService, fetchFunction))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(dateGroupTasks.toArray(new CompletableFuture[0]));
    }
}
