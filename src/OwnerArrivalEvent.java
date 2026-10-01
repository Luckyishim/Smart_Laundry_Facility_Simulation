/**
 * OwnerArrivalEvent.java — Bonus / Optional Congestion Handler.
 *
 * Responsibilities:
 * - Handles edge-case: if BOTH kiosks fail simultaneously, customers queue.
 *   When queue reaches 30, a synchronized "Owner Arrival" event unblocks them.
 * - Thread-safety is critical — exactly one thread must trigger the event.
 */


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class OwnerArrivalEvent {

    private static final int QUEUE_THRESHOLD = 30;
    private final AtomicInteger waitingCustomers = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition ownerArrived = lock.newCondition();
    private final Runnable onOwnerArrival;
    private boolean ownerHasArrived = false;

    public OwnerArrivalEvent() {
        this(null);
    }

    public OwnerArrivalEvent(Runnable onOwnerArrival) {
        this.onOwnerArrival = onOwnerArrival;
    }

    public void customerWaiting() throws InterruptedException {
        lock.lock();
        try {
            if (ownerHasArrived) {
                return;
            }
            int count = waitingCustomers.incrementAndGet();
            LaundryLogger.log("QUEUE", "Payment kiosk outage! Customer queued for owner (" + count + "/" + QUEUE_THRESHOLD + ")");
            if (count >= QUEUE_THRESHOLD) {
                triggerOwnerArrival();
            } else {
                while (!ownerHasArrived) {
                    ownerArrived.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void triggerOwnerArrival() {
        ownerHasArrived = true;
        LaundryLogger.log("OWNER", "Owner has arrived! Resolving congestion for 30 customers.");
        if (onOwnerArrival != null) {
            onOwnerArrival.run();
        }
        ownerArrived.signalAll();
        waitingCustomers.set(0);
    }

    public void customerResolved() {
        waitingCustomers.decrementAndGet();
    }
}

