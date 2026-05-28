import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ferry {
    private static final int MAX_CAPACITY = 20;
    private static final int TRAVEL_TIME_MS = 800;
    private static final int MAX_WAIT_MS = 3000;

    private Side currentSide;
    private int currentLoad = 0;
    private final List<Vehicle> loadedVehicles = new ArrayList<>();

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

    /** New method: loads as many vehicles as possible from the current waiting area. **/
    /**
     * Blocks until ferry should depart (full, next doesn't fit, or timeout).
     * While waiting, it loads arriving vehicles atomically to avoid busy-waiting
     * and to ensure mutual exclusion over ferry state.
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
                        currentLoad += v.getType().getCapacity();
                        loadedVehicles.add(v);
                        Logger.log(String.format("Ferry loaded %s (capacity=%d, current load=%d/%d)",
                                v, v.getType().getCapacity(), currentLoad, MAX_CAPACITY));
                    } else {
                        break;
                    }
                }

                // Decide departure
                if (currentLoad == MAX_CAPACITY) {
                    Logger.log("Ferry is full → departing");
                    return;
                }

                Vehicle next = currentWA.peekNext();
                if (next != null && currentLoad + next.getType().getCapacity() > MAX_CAPACITY) {
                    Logger.log(String.format("Next vehicle %s cannot fit (%d+%d > %d) → departing",
                            next, currentLoad, next.getType().getCapacity(), MAX_CAPACITY));
                    return;
                }

                // Wait for more vehicles or timeout (prevents starvation)
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    Logger.log("Timeout reached → departing to prevent starvation");
                    return;
                }
                // Wait to be signalled that a vehicle arrived or until timeout
                departureCondition.await(Math.min(remaining, MAX_WAIT_MS), TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public void departAndTravel() throws InterruptedException {
        // --- Record trip statistics BEFORE travel ---
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            long waitTime = now - lastArrivalTime;       // time spent at dock this side
            // Pass trip data to Statistics
            stats.recordTrip(currentSide, currentLoad, loadedVehicles.size(), waitTime);
            Logger.log(String.format("Ferry departed from Side %s with %d units",
                    currentSide, currentLoad));
        } finally {
            lock.unlock();
        }

        // Simulate travel
        Thread.sleep(TRAVEL_TIME_MS);

        // Arrive at opposite side
        currentSide = currentSide.getOpposite();
        Logger.log(String.format("Ferry arrived at Side %s", currentSide));

        // Unload all vehicles
        unloadAll();

        // After unloading, the ferry is ready for new boarding
        lock.lock();
        try {
            canLoad.signalAll();
            // Update the arrival time AFTER signalling – this is the moment
            // the ferry is fully available on the new side.
            lastArrivalTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    private void unloadAll() throws InterruptedException {
        lock.lock();
        try {
            unloading = true;
            Logger.log(String.format("Ferry unloading %d vehicles on Side %s",
                    loadedVehicles.size(), currentSide));

            for (Vehicle v : loadedVehicles) {
                Logger.log(String.format("%s unloaded on Side %s", v, currentSide));
                v.setCurrentSide(currentSide);
                synchronized (v) {
                    v.notify();
                }
                Thread.sleep(50);
            }

            loadedVehicles.clear();
            currentLoad = 0;
            unloading = false;
        } finally {
            lock.unlock();
        }
    }

    public void signalVehicleArrived() {
        lock.lock();
        try {
            // Broadcast to wake any ferry waiter so it can load atomically
            departureCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Side getCurrentSide() { return currentSide; }
    public boolean isRunning()   { return running; }
    public void stop()           { running = false; }
}