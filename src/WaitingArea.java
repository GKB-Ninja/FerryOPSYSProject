import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WaitingArea {
    private final Side side;
    private final Queue<Vehicle> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    // Condition to wake up the ferry if it's waiting for vehicles
    private final Condition notEmpty = lock.newCondition();

    public WaitingArea(Side side) {
        this.side = side;
    }

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

    // Safely removes and returns the front vehicle (called by Ferry during boarding)
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

    // Allows the ferry to check the capacity of the next vehicle without removing it
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