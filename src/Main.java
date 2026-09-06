/**
 * Main.java — Entry Point for the Smart Laundry Facility Simulation.
 *
 * Responsibilities:
 * - Sets up an ExecutorService / fixed thread pool to manage Customer threads.
 * - Staggers 50 Customer thread arrivals with a random delay of 0–3 seconds.
 * - Uses a CountDownLatch(50) to detect when all customers have completed
 *   their full wash → dry → pay lifecycle.
 * - Once the latch reaches zero, triggers MetricsReporter to print the final report.
 * - Gracefully shuts down the ExecutorService after the simulation completes.
 */


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;



public class Main {

    private static final int TOTAL_CUSTOMERS = 50;

    public static void main(String[] args) throws InterruptedException {
        SimulationMetrics metrics  = new SimulationMetrics();
        LaundryFacility   facility = new LaundryFacility(metrics);
        CountDownLatch    latch    = new CountDownLatch(TOTAL_CUSTOMERS);
        ExecutorService   executor = Executors.newFixedThreadPool(TOTAL_CUSTOMERS);

        // Launch the real-time GUI dashboard (non-blocking — JFrame created on EDT)
        LaundryGUI.launch(facility, metrics);

        LaundryLogger.log("SYSTEM", "=== Smart Laundry Facility Simulation Started ===");
        LaundryLogger.log("SYSTEM", "Resources: 6 Washers | 4 Dryers | 2 Kiosks");
        LaundryLogger.log("SYSTEM", "Customers: " + TOTAL_CUSTOMERS);
        System.out.println();

        for (int i = 0; i < TOTAL_CUSTOMERS; i++) {
            // Stagger arrivals: random 0–3 second delay between customers
            long delay = ThreadLocalRandom.current().nextLong(0, 3001);
            TimeUnit.MILLISECONDS.sleep(delay);
            executor.submit(new Customer(i, facility, metrics, latch));
        }

        // Wait for ALL customers to finish
        latch.await();

        // Print final report
        MetricsReporter.printReport(metrics);

        // Graceful shutdown
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        LaundryLogger.log("SYSTEM", "=== Simulation Complete ===");
    }
}
