import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the ferry's state, loading, unloading, and travel.
 * Uses ReentrantLock and Condition variables to ensure mutual exclusion and prevent busy-waiting.
 */
public class Ferry {
    private static final int MAX_CAPACITY = 20;
    private static final int MAX_WAIT_MS = 3000;

    private Side currentSide;
    private int currentLoad = 0;
    private final List<Vehicle> loadedVehicles = new ArrayList<>();

    // Synchronization primitives
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition canLoad = lock.newCondition();
    private final Condition departureCondition = lock.newCondition();

    private volatile boolean unloading = false;
    private volatile boolean running = true;

    private final WaitingArea[] waitingAreas = new WaitingArea[2];
    private final Statistics stats;
    private long lastArrivalTime = System.currentTimeMillis();

    public Ferry(Side startSide, WaitingArea sideA, WaitingArea sideB, Statistics stats) {
        this.currentSide = startSide;
        this.waitingAreas[Side.A.ordinal()] = sideA;
        this.waitingAreas[Side.B.ordinal()] = sideB;
        this.stats = stats;
        Logger.log(String.format("Ferry created. Starting at Side %s", startSide));
    }

    /**
     * Attempts to load vehicles from the current side's waiting area.
     * Blocks until departure conditions (capacity, fit, or timeout) are met.
     */
    public void boardAndDecideDeparture() throws InterruptedException {
        lock.lock();
        try {
            if (unloading) return;
            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
            WaitingArea currentWA = waitingAreas[currentSide.ordinal()];

            while (true) {
                // Load as many as possible from current waiting area queue
                while (true) {
                    Vehicle next = currentWA.peekNext();
                    if (next == null) break;
                    if (currentLoad + next.getType().getCapacity() > MAX_CAPACITY) break;

                    Vehicle v = currentWA.getNext();
                    if (v != null) {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); // Random delay for boarding

                        currentLoad += v.getType().getCapacity();
                        loadedVehicles.add(v);
                        Logger.log(String.format("Ferry loaded %s (capacity=%d, current load=%d/%d)",
                                v, v.getType().getCapacity(), currentLoad, MAX_CAPACITY));
                    } else {
                        break;
                    }
                }

                // Condition 1: Depart if maximum capacity is reached
                if (currentLoad == MAX_CAPACITY) {
                    Logger.log("Ferry is full → departing");
                    return;
                }

                // Condition 2: Depart if the next vehicle in queue exceeds remaining capacity
                Vehicle next = currentWA.peekNext();
                if (next != null && currentLoad + next.getType().getCapacity() > MAX_CAPACITY) {
                    Logger.log(String.format("Next vehicle %s cannot fit (%d+%d > %d) → departing",
                            next, currentLoad, next.getType().getCapacity(), MAX_CAPACITY));
                    return;
                }

                // Condition 3: Depart if max wait time is reached (Fairness and Starvation prevention)
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    Logger.log("Timeout reached → departing to prevent starvation");
                    return;
                }
                departureCondition.await(Math.min(remaining, MAX_WAIT_MS), TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Simulates the ferry's travel across the water and triggers the unloading sequence upon arrival.
     */
    public void logAndTravel() throws InterruptedException {
        long travelTimeMs = ThreadLocalRandom.current().nextInt(700, 1000); // Random travel delay

        lock.lock();
        try {
            long ferryWaitTimeMs = System.currentTimeMillis() - lastArrivalTime;
            stats.recordTrip(currentSide, currentLoad, loadedVehicles.size(), ferryWaitTimeMs, travelTimeMs);
            Logger.log(String.format("Ferry departed from Side %s with %d units", currentSide, currentLoad));
        } finally {
            lock.unlock();
        }

        Thread.sleep(travelTimeMs);

        currentSide = currentSide.getOpposite();
        Logger.log(String.format("Ferry arrived at Side %s", currentSide));

        unloadAll();

        lock.lock();
        try {
            canLoad.signalAll();
            lastArrivalTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Empties the ferry entirely before allowing any new vehicles to board (Strict Loading/Unloading policy).
     */
    private void unloadAll() throws InterruptedException {
        lock.lock();
        try {
            unloading = true;
            Logger.log(String.format("Ferry unloading %d vehicles on Side %s", loadedVehicles.size(), currentSide));

            for (Vehicle v : loadedVehicles) {
                Logger.log(String.format("%s unloaded on Side %s", v, currentSide));
                v.setCurrentSide(currentSide);

                // Wake up the specific vehicle thread so it can continue its lifecycle
                synchronized (v) {
                    v.notify();
                }
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); // Delay for unloading
            }

            loadedVehicles.clear();
            currentLoad = 0;
            unloading = false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wakes up the ferry thread when a new vehicle enters the waiting area.
     */
    public void signalVehicleArrived() {
        lock.lock();
        try {
            departureCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Side getCurrentSide() { return currentSide; }
    public boolean isRunning()   { return running; }
    public void stop()           { running = false; }
}