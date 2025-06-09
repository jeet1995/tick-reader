package com.tickreader.service.strategy;

import com.azure.cosmos.CosmosException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class RetryOnSpecificExceptionStrategy implements ErrorHandlingStrategy {

    private final Set<Integer> allowedStatusCodes;

    public RetryOnSpecificExceptionStrategy() {
        this.allowedStatusCodes = new HashSet<>();

        this.allowedStatusCodes.add(408);
        this.allowedStatusCodes.add(449);
        this.allowedStatusCodes.add(429);
        this.allowedStatusCodes.add(503);
    }

    @Override
    public <T> Flux<T> apply(Flux<T> sourceFlux) {
        return sourceFlux.retryWhen(
                Retry.backoff(3, Duration.ofMillis(500))
                        .filter(throwable -> {
                            if (throwable instanceof CosmosException) {
                                CosmosException cosmosException = (CosmosException) throwable;

                                int statusCode = cosmosException.getStatusCode();

                                return this.allowedStatusCodes.contains(statusCode);
                            }

                            return false;
                        })
        );
    }
}
