/**
 * SimulationMetrics.java — Thread-Safe Simulation Statistics Tracker.
 *
 * Responsibilities:
 * - Maintains atomic counters for real-time and aggregate statistics:
 *     • totalServed, totalTimeAccumulated, currentWashersInUse,
 *       maxWashersInUse, currentDryersInUse, maxDryersInUse.
 * - Exposes thread-safe mutator methods called from LaundryFacility.
 * - increment* methods atomically update current count AND CAS-update the max.
 * - recordCustomerServed(long elapsedMs) increments totalServed and accumulates time.
 * - Exposes read-only getters for MetricsReporter.
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SimulationMetrics {

    private final AtomicInteger totalServed          = new AtomicInteger(0);
    private final AtomicLong    totalTimeAccumulated  = new AtomicLong(0);

    private final AtomicInteger currentWashersInUse   = new AtomicInteger(0);
    private final AtomicInteger maxWashersInUse       = new AtomicInteger(0);
    private final AtomicInteger currentDryersInUse    = new AtomicInteger(0);
    private final AtomicInteger maxDryersInUse        = new AtomicInteger(0);

    // --- Washer concurrency tracking ---
    public void incrementWashersInUse() {
        int current = currentWashersInUse.incrementAndGet();
        // CAS loop to update max
        int prevMax;
        do {
            prevMax = maxWashersInUse.get();
        } while (current > prevMax && !maxWashersInUse.compareAndSet(prevMax, current));
    }

    public void decrementWashersInUse() {
        currentWashersInUse.decrementAndGet();
    }

    // --- Dryer concurrency tracking ---
    public void incrementDryersInUse() {
        int current = currentDryersInUse.incrementAndGet();
        int prevMax;
        do {
            prevMax = maxDryersInUse.get();
        } while (current > prevMax && !maxDryersInUse.compareAndSet(prevMax, current));
    }

    public void decrementDryersInUse() {
        currentDryersInUse.decrementAndGet();
    }

    // --- Customer completion ---
    public void recordCustomerServed(long elapsedTimeMs) {
        totalServed.incrementAndGet();
        totalTimeAccumulated.addAndGet(elapsedTimeMs);
    }

    // --- Getters for MetricsReporter ---
    public int getTotalServed()       { return totalServed.get(); }
    public int getMaxWashersInUse()   { return maxWashersInUse.get(); }
    public int getMaxDryersInUse()    { return maxDryersInUse.get(); }
    public int getCurrentWashersInUse() { return currentWashersInUse.get(); }
    public int getCurrentDryersInUse()  { return currentDryersInUse.get(); }

    public double getAverageTimeMs() {
        int served = totalServed.get();
        return served == 0 ? 0.0 : (double) totalTimeAccumulated.get() / served;
    }
}
