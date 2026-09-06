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

    public void customerWaiting() {
        // TODO: Increment waitingCustomers
        // TODO: If threshold reached → triggerOwnerArrival()
        // TODO: Otherwise → block this thread until owner arrives
    }

    private void triggerOwnerArrival() {
        // TODO: Log "Owner has arrived! Resolving congestion for 30 customers."
        // TODO: Unblock all waiting threads via ownerArrived.signalAll()
        // TODO: Reset counter
    }

    public void customerResolved() {
        // TODO: Decrement counter, any cleanup
    }
}
