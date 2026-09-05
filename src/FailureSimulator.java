/**
 * FailureSimulator.java — Centralised Probability Roll Logic.
 *
 * Responsibilities:
 * - Encapsulates all random failure-chance checks used during the simulation.
 * - Uses ThreadLocalRandom for thread-safe, contention-free random number generation.
 * - Exposes static methods:
 *     • boolean rollWasherFailure()  — returns true with 5% probability.
 *     • boolean rollKioskFailure()   — returns true with 5% probability.
 * - Failure percentages are defined as named constants for easy tuning.
 * - Stateless utility class — no instance fields.
 */

import java.util.concurrent.ThreadLocalRandom;

public class FailureSimulator {

    private static final double WASHER_FAILURE_RATE = 0.05;
    private static final double KIOSK_FAILURE_RATE  = 0.05;

    private FailureSimulator() { /* utility class */ }

    public static boolean rollWasherFailure() {
        return ThreadLocalRandom.current().nextDouble() < WASHER_FAILURE_RATE;
    }

    public static boolean rollKioskFailure() {
        return ThreadLocalRandom.current().nextDouble() < KIOSK_FAILURE_RATE;
    }
}
