package com.tickreader.service.strategy;

import com.azure.cosmos.CosmosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class RetryOnSpecificExceptionStrategy implements ErrorHandlingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RetryOnSpecificExceptionStrategy.class);

    private final Set<Integer> retriableStatusCodes;

    public RetryOnSpecificExceptionStrategy() {
        this.retriableStatusCodes = new HashSet<>();

        this.retriableStatusCodes.add(408);
        this.retriableStatusCodes.add(449);
        this.retriableStatusCodes.add(429);
        this.retriableStatusCodes.add(500);
        this.retriableStatusCodes.add(503);
    }

    @Override
    public <T> Flux<T> apply(Flux<T> sourceFlux) {
        return sourceFlux.retryWhen(Retry.backoff(3, Duration.ofMillis(500)).filter(throwable -> {
            if (throwable instanceof CosmosException) {
                CosmosException cosmosException = (CosmosException) throwable;

                int statusCode = cosmosException.getStatusCode();

                return this.retriableStatusCodes.contains(statusCode);
            }

            return false;
        }).doAfterRetry(signal -> {

            Throwable throwable = signal.failure();

            if (throwable instanceof CosmosException) {
                CosmosException cosmosException = (CosmosException) throwable;

                logger.warn("Retry count : {} for status code {}", cosmosException.getStatusCode(), signal.totalRetries());
            }
        }));
    }
}
