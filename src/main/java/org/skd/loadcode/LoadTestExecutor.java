package org.skd.loadcode;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

    private ThreadPoolExecutor executor;

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
        this.executor = new ThreadPoolExecutor(threads, threads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        executor.setKeepAliveTime(10, TimeUnit.SECONDS);

        long startTime = System.currentTimeMillis();
        long endTime = (testDuration > 0) ? startTime + (testDuration * 1000L) : Long.MAX_VALUE;

        for (int i = 0; i < threads; i++) {
            addThread(i, endTime);
        }

        // **New logic: Automatically shut down when all iterations finish**
        monitorAndShutdown();
    }

    private void addThread(int threadIndex, long endTime) {
        executor.submit(() -> {
            try {
                int rampUpDelay = (rampUpTime > 0) ? (rampUpTime * 1000) / threads : 0;
                Thread.sleep(rampUpDelay * threadIndex);

                int currentIteration = 0;

                while (!stopTest && (iterations == -1 || currentIteration < iterations)) {
                    if (System.currentTimeMillis() > endTime) break;

                    totalIterations.incrementAndGet();
                    testMethod.invoke(testInstance, this);
                    currentIteration++;
                }

                System.out.println("Thread " + Thread.currentThread().getName() + " finished.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void addNewThreads(int numberOfThreads) {
        System.out.println("Adding " + numberOfThreads + " new threads...");

        int newThreadCount = threads + numberOfThreads;
        executor.setCorePoolSize(newThreadCount);
        executor.setMaximumPoolSize(newThreadCount);
        threads = newThreadCount;

        long endTime = (testDuration > 0) ? System.currentTimeMillis() + (testDuration * 1000L) : Long.MAX_VALUE;
        for (int i = 0; i < numberOfThreads; i++) {
            addThread(i, endTime);
        }
    }

    public int getTotalThreads() {
        return executor.getActiveCount();
    }

    public int getTotalIterations() {
        return totalIterations.get();
    }

    public void shutdownService() {
        System.out.println("Shutting down LoadTestExecutor...");
        stopTest = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public void stop() {
        this.stopTest = true;
    }

    // **New method: Auto-detect completion and shut down**
    private void monitorAndShutdown() {
        new Thread(() -> {
            try {
                while (!executor.isTerminated()) {
                    if (iterations != -1 && executor.getActiveCount() == 0) {
                        System.out.println("All iterations completed. Shutting down...");
                        shutdownService();
                        break;
                    }
                    Thread.sleep(1000); // Check every second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Load test monitor interrupted.");
            }
        }).start();
    }
}
