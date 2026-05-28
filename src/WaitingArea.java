import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the FIFO queue for a single side of the ferry crossing.
 * Enforces arrival ordering and handles thread safety for joining and leaving the queue.
 */
public class WaitingArea {
    private final Side side;
    private final Queue<Vehicle> queue = new LinkedList<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public WaitingArea(Side side) {
        this.side = side;
    }

    /**
     * Appends a vehicle to the end of the queue (FIFO enforcement).
     */
    public void enter(Vehicle vehicle) {
        lock.lock();
        try {
            vehicle.setQueueEntryTime(System.currentTimeMillis());
            queue.add(vehicle);
            Logger.log(String.format("%s joined queue on Side %s (position %d)",
                    vehicle, side, queue.size()));
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the next vehicle in line, recording its final wait time.
     */
    public Vehicle getNext() {
        lock.lock();
        try {
            Vehicle v = queue.poll();
            if (v != null) {
                v.setBoardingStartTime(System.currentTimeMillis());
                v.computeAndAddWaitTime();
                Logger.log(String.format("%s taken from queue on Side %s", v, side));
            }
            return v;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Observes the next vehicle in line without removing it (used to verify ferry capacity limit).
     */
    public Vehicle peekNext() {
        lock.lock();
        try {
            return queue.peek();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}