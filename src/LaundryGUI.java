/**
 * LaundryGUI.java — Real-time Swing dashboard for the Smart Laundry Facility Simulation.
 *
 * Thread-safety strategy:
 * ─────────────────────
 * 1. All Swing component creation happens on the EDT via SwingUtilities.invokeLater().
 * 2. Log updates arrive from background Customer threads and are dispatched to the
 *    EDT via SwingUtilities.invokeLater() inside the LaundryLogger listener callback.
 *    This ensures no background thread ever touches a Swing component directly.
 * 3. Resource status and metrics are polled every 500 ms using a javax.swing.Timer,
 *    which fires its ActionListener on the EDT by design — no raw polling threads
 *    or manual synchronisation required on the UI side.
 * 4. The facility/metrics getters are individually thread-safe (synchronized blocks
 *    or Atomic* variables), so reading them from the EDT timer is safe and non-blocking.
 */

import LaundryFacility;
import LaundryLogger;
import SimulationMetrics;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class LaundryGUI {

    // ── Simulation constants (must match Main / LaundryFacility) ──────────
    private static final int TOTAL_CUSTOMERS = 50;
    private static final int WASHER_COUNT    = 6;
    private static final int DRYER_COUNT     = 4;
    private static final int KIOSK_COUNT     = 2;

    // ── Colour palette ───────────────────────────────────────────────────
    private static final Color CLR_FREE     = new Color(76, 175, 80);   // Material Green 500
    private static final Color CLR_IN_USE   = new Color(244, 67, 54);   // Material Red 500
    private static final Color CLR_PANEL_BG = new Color(250, 250, 250);
    private static final Color CLR_LOG_BG   = new Color(30, 30, 30);
    private static final Color CLR_LOG_FG   = new Color(204, 204, 204);
    private static final Color CLR_HEADER   = new Color(55, 71, 79);    // Blue-grey 800
    private static final Color CLR_COMPLETE = new Color(46, 125, 50);   // Green 800

    // ── Polling interval ─────────────────────────────────────────────────
    private static final int POLL_INTERVAL_MS = 500;

    // ── UI Components ────────────────────────────────────────────────────
    private final ResourceIndicator[] washerInd = new ResourceIndicator[WASHER_COUNT];
    private final ResourceIndicator[] dryerInd  = new ResourceIndicator[DRYER_COUNT];
    private final ResourceIndicator[] kioskInd  = new ResourceIndicator[KIOSK_COUNT];

    private JProgressBar washerBar;
    private JProgressBar dryerBar;
    private JProgressBar kioskBar;
    private JProgressBar overallBar;

    private JTextArea logArea;

    private JLabel servedLabel;
    private JLabel avgTimeLabel;
    private JLabel maxWashersLabel;
    private JLabel maxDryersLabel;
    private JLabel statusLabel;

    private Timer pollingTimer;

    // ── Simulation references (read-only from the GUI's perspective) ─────
    private final LaundryFacility facility;
    private final SimulationMetrics metrics;

    // ─────────────────────────────────────────────────────────────────────
    // Constructor (private — use launch())
    // ─────────────────────────────────────────────────────────────────────
    private LaundryGUI(LaundryFacility facility, SimulationMetrics metrics) {
        this.facility = facility;
        this.metrics  = metrics;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point — called from Main.java
    // ─────────────────────────────────────────────────────────────────────
    /**
     * Creates and shows the GUI dashboard. Returns immediately (non-blocking);
     * the JFrame is constructed on the EDT via {@code SwingUtilities.invokeLater}.
     *
     * @param facility the shared LaundryFacility whose resource state is polled
     * @param metrics  the shared SimulationMetrics whose counters are polled
     */
    public static void launch(LaundryFacility facility, SimulationMetrics metrics) {
        /*
         * SwingUtilities.invokeLater posts the Runnable to the EDT's event queue
         * and returns immediately, so the calling thread (main) is never blocked.
         * The JFrame, all panels, the log listener, and the polling Timer are all
         * created inside the Runnable — guaranteeing they run on the EDT.
         */
        SwingUtilities.invokeLater(() -> {
            // Attempt Nimbus look-and-feel for a modern appearance
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) { /* proceed with default L&F */ }

            LaundryGUI gui = new LaundryGUI(facility, metrics);
            gui.buildAndShow();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Build the complete UI and make it visible
    // ─────────────────────────────────────────────────────────────────────
    private void buildAndShow() {
        JFrame frame = new JFrame("Smart Laundry Facility Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(920, 680);
        frame.setMinimumSize(new Dimension(750, 550));
        frame.setLocationRelativeTo(null); // centre on screen

        // Main content panel with padding
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        content.setBackground(CLR_PANEL_BG);

        content.add(createResourcePanel(), BorderLayout.NORTH);
        content.add(createLogPanel(),      BorderLayout.CENTER);
        content.add(createBottomPanel(),   BorderLayout.SOUTH);

        frame.setContentPane(content);

        // Wire the log listener and start the polling timer
        wireLogListener();
        startPollingTimer();

        frame.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NORTH — Resource Status Panel
    // ═════════════════════════════════════════════════════════════════════
    private JPanel createResourcePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        washerBar = makeBar(WASHER_COUNT);
        dryerBar  = makeBar(DRYER_COUNT);
        kioskBar  = makeBar(KIOSK_COUNT);

        panel.add(createResourceRow("Washers",        WASHER_COUNT, washerInd, washerBar));
        panel.add(Box.createVerticalStrut(4));
        panel.add(createResourceRow("Dryers",         DRYER_COUNT,  dryerInd,  dryerBar));
        panel.add(Box.createVerticalStrut(4));
        panel.add(createResourceRow("Payment Kiosks", KIOSK_COUNT,  kioskInd,  kioskBar));

        return panel;
    }

    /**
     * Builds one resource row:  [ ●1  ●2  ●3 ... ]   ████░░░ X/Y in use
     */
    private JPanel createResourceRow(String title, int count,
                                     ResourceIndicator[] indicators,
                                     JProgressBar bar) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)),
                        " " + title + " (" + count + ") ",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 12), CLR_HEADER),
                BorderFactory.createEmptyBorder(4, 8, 6, 8)));
        row.setOpaque(false);

        // Left: coloured indicators in a flow layout
        JPanel dots = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dots.setOpaque(false);
        // Determine the resource-type prefix for tooltips (first word of title)
        String resourceType = title.split(" ")[0];
        for (int i = 0; i < count; i++) {
            indicators[i] = new ResourceIndicator(resourceType, i);
            dots.add(indicators[i]);
        }
        row.add(dots, BorderLayout.CENTER);

        // Right: progress bar showing "X / Y in use"
        bar.setPreferredSize(new Dimension(200, 26));
        bar.setString("0 / " + count + " in use");
        row.add(bar, BorderLayout.EAST);

        return row;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CENTRE — Live Activity Log
    // ═════════════════════════════════════════════════════════════════════
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Live Activity Log ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12), CLR_HEADER));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(CLR_LOG_BG);
        logArea.setForeground(CLR_LOG_FG);
        logArea.setCaretColor(CLR_LOG_FG);
        logArea.setMargin(new Insets(6, 8, 6, 8));

        /*
         * DefaultCaret.ALWAYS_UPDATE makes the viewport auto-scroll to follow
         * new text appended at the bottom — no manual caret repositioning needed.
         */
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SOUTH — Progress Bar + Metrics Grid
    // ═════════════════════════════════════════════════════════════════════
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        // ── Overall progress bar ──
        overallBar = new JProgressBar(0, TOTAL_CUSTOMERS);
        overallBar.setStringPainted(true);
        overallBar.setString("0 / " + TOTAL_CUSTOMERS + " Customers Served");
        overallBar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        overallBar.setPreferredSize(new Dimension(0, 30));
        panel.add(overallBar, BorderLayout.NORTH);

        // ── Metrics grid (2 rows × 3 columns) ──
        JPanel grid = new JPanel(new GridLayout(2, 3, 20, 4));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));

        Font metricFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

        servedLabel     = new JLabel("Customers Served: 0 / " + TOTAL_CUSTOMERS);
        avgTimeLabel    = new JLabel("Avg Time / Customer: 0.0 s");
        maxWashersLabel = new JLabel("Peak Washers: 0 / " + WASHER_COUNT);
        maxDryersLabel  = new JLabel("Peak Dryers: 0 / " + DRYER_COUNT);
        statusLabel     = new JLabel("\u23F3 Simulation Running...");  // ⏳

        for (JLabel lbl : new JLabel[]{servedLabel, avgTimeLabel, maxWashersLabel,
                maxDryersLabel, statusLabel}) {
            lbl.setFont(metricFont);
        }
        statusLabel.setFont(metricFont.deriveFont(Font.BOLD));
        statusLabel.setForeground(new Color(245, 124, 0)); // Orange

        grid.add(servedLabel);
        grid.add(avgTimeLabel);
        grid.add(statusLabel);
        grid.add(maxWashersLabel);
        grid.add(maxDryersLabel);
        grid.add(new JLabel()); // empty cell for alignment

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Wire LaundryLogger listener  (log lines → JTextArea)
    // ═════════════════════════════════════════════════════════════════════
    private void wireLogListener() {
        /*
         * LaundryLogger.addListener registers a Consumer<String> that fires on
         * whatever thread calls log() — typically a Customer background thread.
         * We MUST NOT touch Swing components on that thread, so the actual
         * JTextArea.append is wrapped in SwingUtilities.invokeLater() to safely
         * post the update to the EDT's event queue.
         */
        LaundryLogger.addListener(line ->
                SwingUtilities.invokeLater(() -> logArea.append(line + "\n"))
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Polling Timer  (fires every 500 ms on the EDT)
    // ═════════════════════════════════════════════════════════════════════
    private void startPollingTimer() {
        /*
         * javax.swing.Timer guarantees its ActionListener fires on the EDT,
         * so reading simulation state and updating Swing components here
         * requires no additional synchronisation on the UI side.
         */
        pollingTimer = new Timer(POLL_INTERVAL_MS, e -> updateAll());
        pollingTimer.setInitialDelay(0); // immediate first tick
        pollingTimer.start();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Refresh all UI components  (called on the EDT by the Timer)
    // ═════════════════════════════════════════════════════════════════════
    private void updateAll() {

        // ── Washer indicators + progress bar ──
        int washersInUse = metrics.getCurrentWashersInUse();
        for (int i = 0; i < WASHER_COUNT; i++) {
            washerInd[i].setAvailable(facility.isWasherAvailable(i));
        }
        washerBar.setValue(washersInUse);
        washerBar.setString(washersInUse + " / " + WASHER_COUNT + " in use");

        // ── Dryer indicators + progress bar ──
        int dryersInUse = metrics.getCurrentDryersInUse();
        for (int i = 0; i < DRYER_COUNT; i++) {
            dryerInd[i].setAvailable(facility.isDryerAvailable(i));
        }
        dryerBar.setValue(dryersInUse);
        dryerBar.setString(dryersInUse + " / " + DRYER_COUNT + " in use");

        // ── Kiosk indicators + progress bar (no metrics counter — compute from array) ──
        int kiosksInUse = 0;
        for (int i = 0; i < KIOSK_COUNT; i++) {
            boolean avail = facility.isKioskAvailable(i);
            kioskInd[i].setAvailable(avail);
            if (!avail) kiosksInUse++;
        }
        kioskBar.setValue(kiosksInUse);
        kioskBar.setString(kiosksInUse + " / " + KIOSK_COUNT + " in use");

        // ── Aggregate metrics ──
        int    served = metrics.getTotalServed();
        double avgMs  = metrics.getAverageTimeMs();

        overallBar.setValue(served);
        overallBar.setString(served + " / " + TOTAL_CUSTOMERS + " Customers Served");

        servedLabel.setText("Customers Served: " + served + " / " + TOTAL_CUSTOMERS);
        avgTimeLabel.setText(String.format("Avg Time / Customer: %.1f s", avgMs / 1000.0));
        maxWashersLabel.setText("Peak Washers: " + metrics.getMaxWashersInUse() + " / " + WASHER_COUNT);
        maxDryersLabel.setText("Peak Dryers: " + metrics.getMaxDryersInUse() + " / " + DRYER_COUNT);

        // ── Detect simulation completion ──
        if (served >= TOTAL_CUSTOMERS) {
            statusLabel.setText("\u2705 Simulation Complete!");  // ✅
            statusLabel.setForeground(CLR_COMPLETE);
            overallBar.setForeground(CLR_COMPLETE);
            pollingTimer.stop(); // no need to keep polling
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helper — create a configured JProgressBar
    // ─────────────────────────────────────────────────────────────────────
    private static JProgressBar makeBar(int max) {
        JProgressBar bar = new JProgressBar(0, max);
        bar.setStringPainted(true);
        bar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Inner class — ResourceIndicator  (custom-painted rounded square)
    // ═════════════════════════════════════════════════════════════════════
    /**
     * A small custom-painted panel displaying a numbered rounded square.
     * <ul>
     *   <li>Green ({@link #CLR_FREE}) = resource is available</li>
     *   <li>Red   ({@link #CLR_IN_USE}) = resource is occupied</li>
     * </ul>
     * Repaints only when the availability state actually changes, avoiding flicker.
     */
    private static class ResourceIndicator extends JPanel {
        private boolean available = true;
        private final String label;     // display text inside the square, e.g. "3"
        private final String fullName;  // tooltip text, e.g. "Washer 3"

        ResourceIndicator(String resourceType, int zeroBasedId) {
            this.label    = String.valueOf(zeroBasedId + 1);       // 1-based display
            this.fullName = resourceType + " " + (zeroBasedId + 1);
            setPreferredSize(new Dimension(38, 38));
            setOpaque(false);
            setToolTipText(fullName + " \u2014 Free");             // — Free
        }

        /** Update availability; repaint only on actual state change. */
        void setAvailable(boolean avail) {
            if (this.available != avail) {
                this.available = avail;
                setToolTipText(fullName + (avail ? " \u2014 Free" : " \u2014 In Use"));
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 3;
            int d   = Math.min(getWidth(), getHeight()) - pad * 2;
            int x   = (getWidth()  - d) / 2;
            int y   = (getHeight() - d) / 2;
            int arc = 10;

            // Filled rounded square
            Color fill = available ? CLR_FREE : CLR_IN_USE;
            g2.setColor(fill);
            g2.fillRoundRect(x, y, d, d, arc, arc);

            // Slightly darker border
            g2.setColor(fill.darker());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, d, d, arc, arc);

            // Centred number label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (d - fm.stringWidth(label)) / 2;
            int ty = y + (d + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, tx, ty);

            g2.dispose();
        }
    }
}
