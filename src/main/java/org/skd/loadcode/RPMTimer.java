package org.skd.loadcode;


public class RPMTimer {
    private final int targetRPM; // Target RPM (requests per minute)
    private final long targetIntervalMillis; // Interval between requests in milliseconds
    private long lastRequestTime = System.currentTimeMillis();

    public RPMTimer(int rpm) {
        if (rpm <= 0) {
            throw new IllegalArgumentException("RPM must be greater than 0.");
        }
        this.targetRPM = rpm;
        this.targetIntervalMillis = 60_000 / rpm; // Calculate the interval between requests
    }

    // Synchronized sleep method to control access across threads
    public synchronized void sleep() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRequestTime;
        long sleepTime = targetIntervalMillis - elapsedTime;

        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis(); // Update the last request time
    }

    public synchronized int getCurrentRPM() {
        return targetRPM;
    }
}
