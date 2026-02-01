package com.stockdownloader.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    private static final Logger LOGGER = Logger.getLogger(RetryExecutorTest.class.getName());

    @Test
    void successfulExecutionOnFirstAttempt() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryExecutor.execute(() -> counter.incrementAndGet(), 3, LOGGER, "test");

        assertEquals(1, counter.get());
    }

    @Test
    void retriesOnFailure() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            counter.incrementAndGet();
            if (counter.get() < 3) {
                throw new RuntimeException("fail");
            }
        }, 3, LOGGER, "test");

        assertEquals(3, counter.get());
    }

    @Test
    void stopsAfterMaxRetries() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            counter.incrementAndGet();
            throw new RuntimeException("always fail");
        }, 2, LOGGER, "test");

        assertEquals(3, counter.get()); // initial + 2 retries
    }

    @Test
    void supplierReturnsResult() {
        String result = RetryExecutor.execute(() -> "hello", 3, LOGGER, "test");
        assertEquals("hello", result);
    }

    @Test
    void supplierRetriesAndReturnsResult() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryExecutor.execute(() -> {
            if (counter.incrementAndGet() < 2) {
                throw new RuntimeException("fail");
            }
            return "success";
        }, 3, LOGGER, "test");

        assertEquals("success", result);
        assertEquals(2, counter.get());
    }

    @Test
    void supplierReturnsNullAfterAllRetriesFail() {
        String result = RetryExecutor.execute(() -> {
            throw new RuntimeException("fail");
        }, 2, LOGGER, "test");

        assertNull(result);
    }

    @Test
    void defaultRetryCountUsed() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            counter.incrementAndGet();
            throw new RuntimeException("fail");
        }, LOGGER, "test");

        assertEquals(4, counter.get()); // initial + 3 default retries
    }
}
