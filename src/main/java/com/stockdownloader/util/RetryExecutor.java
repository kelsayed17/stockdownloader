package com.stockdownloader.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes an action with configurable retry logic.
 */
public final class RetryExecutor {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private RetryExecutor() {}

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static void execute(ThrowingRunnable action, int maxRetries, Logger logger, String context) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.log(Level.FINE, "Retrying {0}, attempt {1}",
                            new Object[]{context, attempt + 1});
                } else {
                    logger.log(Level.WARNING, "Failed {0} after {1} retries: {2}",
                            new Object[]{context, maxRetries, e.getMessage()});
                }
            }
        }
    }

    public static void execute(ThrowingRunnable action, Logger logger, String context) {
        execute(action, DEFAULT_MAX_RETRIES, logger, context);
    }

    public static <T> T execute(ThrowingSupplier<T> action, int maxRetries, Logger logger, String context) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.log(Level.FINE, "Retrying {0}, attempt {1}",
                            new Object[]{context, attempt + 1});
                } else {
                    logger.log(Level.WARNING, "Failed {0} after {1} retries: {2}",
                            new Object[]{context, maxRetries, e.getMessage()});
                }
            }
        }
        return null;
    }
}
