package org.skd.loadcode;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestExecutor {
    private int threads = 1;
    private int iterations = 1; // Use -1 for infinite iterations
    private int rampUpTime = 0; // in seconds
    private int testDuration = 0; // 0 means no duration limit
    private Method testMethod;
    private Object testInstance;
    private final AtomicInteger totalIterations = new AtomicInteger(0);
    private volatile boolean stopTest = false;

    public LoadTestExecutor setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public LoadTestExecutor setIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public LoadTestExecutor setRampUp(int rampUp) {
        this.rampUpTime = rampUp;
        return this;
    }

    public LoadTestExecutor setTestDuration(int duration) {
        this.testDuration = duration;
        return this;
    }

    public LoadTestExecutor addTest(Class<?> testClass, String methodName) throws Exception {
        this.testInstance = testClass.getDeclaredConstructor().newInstance();
        this.testMethod = testClass.getMethod(methodName, LoadTestExecutor.class);
        return this;
    }

    public void start() {
        System.out.println("Starting Load Test...");
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        long startTime = System.currentTimeMillis();
        long endTime = (testDuration > 0) ? startTime + (testDuration * 1000L) : Long.MAX_VALUE;

        for (int i = 0; i < threads; i++) {
            final int threadIndex = i; // Capture thread index to calculate ramp-up delay
            executor.submit(() -> {
                try {
                    // Calculate ramp-up delay for the thread based on its index
                    int rampUpDelay = (rampUpTime > 0) ? (rampUpTime * 1000) / threads : 0;
                    Thread.sleep(rampUpDelay * threadIndex); // Ramp-up delay applied sequentially

                    int currentIteration = 0;

                    // Each thread performs its own iterations
                    while (!stopTest && (iterations == -1 || currentIteration < iterations)) {
                        if (System.currentTimeMillis() > endTime) break;

                        totalIterations.incrementAndGet();  // Increment total iterations across all threads
                        testMethod.invoke(testInstance, this); // Execute the test method

                        currentIteration++;  // Increment this thread's iteration count
                    }

                    // Stop the test after completing the iterations
                    if (iterations != -1 && currentIteration >= iterations) {
                        stopTest = true;
                        shutdownService(executor);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        if (testDuration > 0) {
            try {
                executor.shutdown();
                executor.awaitTermination(testDuration + 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Load test interrupted.");
            }
        } else {
            // If no duration, wait indefinitely until stopped by the user or iterations completed
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow(); // Handle interrupt and shut down immediately
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.err.println("Load test interrupted.");
            }
        }

        System.out.println("Load Test Completed.");
    }

    // Methods for accessing the current state
    public int getTotalThreads() {
        return threads;
    }

    public int getTotalIterations() {
        return totalIterations.get();
    }

    public void shutdownService(ExecutorService executor){
        executor.shutdown();
    }

    public void stop() {
        this.stopTest = true;
    }
}
