import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class TollBooth {
    private final Side side;
    private final int boothId;
    private final Semaphore semaphore = new Semaphore(1);

    public TollBooth(Side side, int boothId) {
        this.side = side;
        this.boothId = boothId;
    }

    public void pass(Vehicle vehicle) throws InterruptedException {
        semaphore.acquire();
        try {
            Logger.log(String.format("%s entered toll on Side %s (booth %d)",
                    vehicle, side, boothId));

            // Random delay: 100–500 ms
            int delay = ThreadLocalRandom.current().nextInt(100, 500);
            Thread.sleep(delay);

            Logger.log(String.format("%s exited toll on Side %s",
                    vehicle, side));
        } finally {
            semaphore.release();
        }
    }
}
