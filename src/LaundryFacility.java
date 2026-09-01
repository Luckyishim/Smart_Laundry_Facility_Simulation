/**
 * LaundryFacility.java — Shared State & Resource Manager.
 *
 * Responsibilities:
 * - Owns three fair Semaphores: washers(6), dryers(4), kiosks(2).
 * - Maintains resource-ID tracking so logs say "Acquired Washer 3".
 * - Exposes acquire/release methods for each resource type.
 * - Each acquire: semaphore.acquire() → find free ID → update metrics → log → return ID.
 * - Each release: mark ID free → semaphore.release() → update metrics → log.
 */

import java.util.concurrent.Semaphore;

public class LaundryFacility {

    private final Semaphore washers = new Semaphore(6, true);
    private final Semaphore dryers  = new Semaphore(4, true);
    private final Semaphore kiosks  = new Semaphore(2, true);

    private final boolean[] washerAvailable = new boolean[6];
    private final boolean[] dryerAvailable  = new boolean[4];
    private final boolean[] kioskAvailable  = new boolean[2];

    private final Object washerLock = new Object();
    private final Object dryerLock  = new Object();
    private final Object kioskLock  = new Object();

    private final SimulationMetrics metrics;

    public LaundryFacility(SimulationMetrics metrics) {
        this.metrics = metrics;
        java.util.Arrays.fill(washerAvailable, true);
        java.util.Arrays.fill(dryerAvailable, true);
        java.util.Arrays.fill(kioskAvailable, true);
    }

    // --- Washer operations ---
    public int acquireWasher() throws InterruptedException {
        washers.acquire();
        int id = claimResource(washerAvailable, washerLock);
        metrics.incrementWashersInUse();
        return id;
    }

    public void releaseWasher(int washerId) {
        synchronized (washerLock) { washerAvailable[washerId] = true; }
        metrics.decrementWashersInUse();
        washers.release();
    }

    // --- Dryer operations ---
    public int acquireDryer() throws InterruptedException {
        dryers.acquire();
        int id = claimResource(dryerAvailable, dryerLock);
        metrics.incrementDryersInUse();
        return id;
    }

    public void releaseDryer(int dryerId) {
        synchronized (dryerLock) { dryerAvailable[dryerId] = true; }
        metrics.decrementDryersInUse();
        dryers.release();
    }

    // --- Kiosk operations ---
    public int acquireKiosk() throws InterruptedException {
        kiosks.acquire();
        int id = claimResource(kioskAvailable, kioskLock);
        return id;
    }

    public void releaseKiosk(int kioskId) {
        synchronized (kioskLock) { kioskAvailable[kioskId] = true; }
        kiosks.release();
    }

    // --- Private helper ---
    private int claimResource(boolean[] available, Object lock) {
        synchronized (lock) {
            for (int i = 0; i < available.length; i++) {
                if (available[i]) {
                    available[i] = false;
                    return i;
                }
            }
        }
        // Should never reach here because semaphore guarantees availability
        throw new IllegalStateException("No resource available despite semaphore permit");
    }

    // --- Availability getters (for GUI polling) ---
    public boolean isWasherAvailable(int id) { synchronized (washerLock) { return washerAvailable[id]; } }
    public boolean isDryerAvailable(int id)  { synchronized (dryerLock)  { return dryerAvailable[id]; } }
    public boolean isKioskAvailable(int id)  { synchronized (kioskLock)  { return kioskAvailable[id]; } }
}
