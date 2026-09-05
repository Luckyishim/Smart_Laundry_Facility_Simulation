/**
 * LaundryLogger.java — Thread-Safe, Timestamped Console Logger.
 *
 * Responsibilities:
 * - Provides a single static synchronized method for all simulation logging:
 *       LaundryLogger.log(String customerId, String message)
 * - Output format:
 *       [HH:mm:ss.SSS] [Customer-12] Acquired Washer 3
 *       [HH:mm:ss.SSS] [Customer-07] Payment failed — retrying in 2s
 * - Synchronization ensures log lines from concurrent threads never interleave
 *   mid-line, producing clean, chronologically ordered console output.
 * - Uses java.time.LocalTime for timestamp formatting.
 * - Stateless utility class — no instance fields.
 */


import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LaundryLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Thread-safe listener list — GUI and other observers register here. */
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private LaundryLogger() { /* utility class */ }

    /**
     * Registers a listener that receives every formatted log line.
     * The listener is invoked on the calling (logging) thread — wrap any
     * Swing updates in {@code SwingUtilities.invokeLater()}.
     */
    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static synchronized void log(String customerId, String message) {
        String timestamp = LocalTime.now().format(FORMATTER);
        String line = String.format("[%s] [%s] %s", timestamp, customerId, message);
        System.out.println(line);
        // Notify registered listeners (e.g., GUI log panel)
        for (Consumer<String> l : listeners) {
            l.accept(line);
        }
    }
}
