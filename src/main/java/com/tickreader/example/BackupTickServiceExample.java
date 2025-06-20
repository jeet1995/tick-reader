package com.tickreader.example;

import com.tickreader.dto.TickResponse;
import com.tickreader.service.BackupTicksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class BackupTickServiceExample {

    @Autowired
    private BackupTicksService backupTicksService;

    public void demonstrateUsage() {
        List<String> rics = Arrays.asList("AAPL.O", "MSFT.O");
        List<String> docTypes = Arrays.asList("QUOTE", "TRADE");
        int totalTicks = 1000;
        boolean pinStart = true;
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // Synchronous usage
        try {
            TickResponse response = backupTicksService.getTicks(
                    rics, docTypes, totalTicks, pinStart, startTime, endTime);
            
            System.out.println("Synchronous response: " + response.getTicks().size() + " ticks");
            System.out.println("Execution time: " + response.getExecutionTime());
            
        } catch (Exception e) {
            System.err.println("Error in synchronous call: " + e.getMessage());
        }

        // Asynchronous usage
        CompletableFuture<TickResponse> futureResponse = backupTicksService.getTicksAsync(
                rics, docTypes, totalTicks, pinStart, startTime, endTime);

        futureResponse
                .thenAccept(response -> {
                    System.out.println("Asynchronous response: " + response.getTicks().size() + " ticks");
                    System.out.println("Execution time: " + response.getExecutionTime());
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in asynchronous call: " + throwable.getMessage());
                    return null;
                });
    }
} 