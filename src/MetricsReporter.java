/**
 * MetricsReporter.java — Final Simulation Report Formatter.
 *
 * Responsibilities:
 * - Reads completed simulation data from SimulationMetrics.
 * - Formats and prints a human-readable summary report to the console.
 * - Called once from Main after the CountDownLatch reaches zero.
 * - Stateless utility class — all methods are static.
 */


public class MetricsReporter {

    private MetricsReporter() { /* utility class */ }

    public static void printReport(SimulationMetrics metrics) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   SMART LAUNDRY FACILITY — FINAL REPORT     ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf ("║  Total Customers Served : %-18d ║%n", metrics.getTotalServed());
        System.out.printf ("║  Avg Time Per Customer  : %-18.1fs║%n", metrics.getAverageTimeMs() / 1000.0);
        System.out.printf ("║  Max Concurrent Washers : %-18s ║%n", metrics.getMaxWashersInUse() + " / 6");
        System.out.printf ("║  Max Concurrent Dryers  : %-18s ║%n", metrics.getMaxDryersInUse()  + " / 4");
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}
