package com.tickreader.service.strategy;

import reactor.core.publisher.Flux;

public interface ErrorHandlingStrategy {
    <T> Flux<T> apply(Flux<T> sourceFlux);
}
