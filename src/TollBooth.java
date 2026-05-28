import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a toll booth using Semaphores for mutual exclusion.
 * Ensures only 1 vehicle passes through a given booth at any one time.
 */
public class TollBooth {
    private final Side side;
    private final int boothId;

    // Strict mutual exclusion limit of 1
    private final Semaphore semaphore = new Semaphore(1);

    public TollBooth(Side side, int boothId) {
        this.side = side;
        this.boothId = boothId;
    }

    /**
     * Vehicle thread attempts to pass the toll, applying a random toll delay.
     */
    public void pass(Vehicle vehicle) throws InterruptedException {
        semaphore.acquire();
        try {
            Logger.log(String.format("%s entered toll on Side %s (booth %d)", vehicle, side, boothId));

            // Random toll delay: 100–500 ms
            int delay = ThreadLocalRandom.current().nextInt(100, 500);
            Thread.sleep(delay);

            Logger.log(String.format("%s exited toll on Side %s", vehicle, side));
        } finally {
            semaphore.release();
        }
    }
}