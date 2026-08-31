import java.util.concurrent.Semaphore;

public class LaundryFacility {
    private final Semaphore washers = new Semaphore(6, true);
    private final Semaphore dryers = new Semaphore(4, true);
    private final Semaphore kiosks = new Semaphore(2, true);

    public Semaphore getWashers() {
        return washers;
    }

    public Semaphore getDryers() {
        return dryers;
    }

    public Semaphore getKiosks() {
        return kiosks;
    }
}
